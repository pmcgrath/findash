(function() {
  'use strict';

  /* Make this a mixin ? */
  var ajax = {
    makeJsonRequest: function(url, method, data, successFn, errorFn) {
      var xhr = new XMLHttpRequest();
      xhr.onreadystatechange = function() {
        if (xhr.readyState == 4) {
          if (xhr.status == 200) {
            var data = JSON.parse(xhr.response);
            successFn(data);
          } else {
            errorFn(xhr);
          }
        }
      };
      xhr.open(method, url);
      xhr.setRequestHeader('Accept', 'application/json');
      if (method === 'GET') {
        xhr.send();
      } else {
        // Assumes there is content - ignoring HEAD, OPTIONS assumes it is a PATCH, POST or PUT
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.send(JSON.stringify(data, null, 2));
      }
    },
    getJsonData: function(url, successFn, errorFn) {
      this.makeJsonRequest(url, 'GET', null, successFn, errorFn);
    },
    postJsonData: function(url, data, successFn, errorFn) {
      this.makeJsonRequest(url, 'POST', data, successFn, errorFn);
    }
  };

  /* Make this a mixin ? Havn't included a de-register method - one instance needs to work for a single socket url */
  var socket = {
    onDisconnectedRetryIntervalInMs: 5000,
    messageHandlers: {},
    open: function(url) {
      var socket = new WebSocket(url);
      socket.onerror = function(error) {
        console.log('socket onerror :' + error.toString());
      };
      socket.onopen = function(event) {
        console.log('socket onopen : Connected to ' + event.currentTarget.url);
      };
      socket.onmessage = function(event) {
        console.log('socket onmessage : Received data');
        var data = JSON.parse(event.data);
        if (this.messageHandlers[data.messageType] != null) {
           this.messageHandlers[data.messageType](data);
        } else {
            console.log('No handler for message-type : ' + data.messageType);
        }
      }.bind(this);
      socket.onclose = function(event) {
        console.log('socket onclose : Disconnected: ' + event.code + ' ' + event.reason);
        socket.onerror = socket.onopen = socket.onmessage = socket.close = socket = null;
        setTimeout(function() { this.open(url); }.bind(this), this.onDisconnectedRetryIntervalInMs);
      }.bind(this);
    },
    registerMessageHandler(messageType, handlerFn) {
      // Only one per message type
      this.messageHandlers[messageType] = handlerFn;
    },
    deregisterMessageHandler(messageType) {
      // Only one per message type
      this.messageHandlers[messageType] = null;
    }
  };

  var App = React.createClass({
    openSocket: function() {
      var url = 'ws://' + this.props.host + '/ws';
      this.props.socket.open(url);
    },
    getCurrenciesList: function() {
      var url = this.props.protocol + '//' + this.props.host + '/api/currencies';
      this.props.ajax.getJsonData(
        url,
        function(currencies) { this.setState({currencies: currencies}); }.bind(this),
        function(xhr)        { console.log('getCurrenciesList error status : ' + xhr.status); });
    },
    getInitialState: function() {
      return {currencies: []};
    },
    componentDidMount: function() {
      this.openSocket();
      this.getCurrenciesList();
    },
    render: function() {
      return (
        <div className="app">
          <Stocks protocol={this.props.protocol} host={this.props.host} currencies={this.state.currencies} ajax={this.props.ajax} socket={this.props.socket} />
          <Currencies protocol={this.props.protocol} host={this.props.host} ajax={this.props.ajax} socket={this.props.socket} />
        </div>
      );
    }
  });

  var Stocks = React.createClass({
    mergeNewQuotesUpdatingState: function(newQuotes) {
      // Create map of existing quotes - key is symbol and value is the existing quote
      var existingQuotesIndex = this.state.quotes.reduce(
        function(accum, quote, index, array) { accum[quote.symbol] = quote; return accum; },
        {});
      // Map the new quotes to a new collection, quotes with existing entries, have the new quote pushed into their existing prices array
      var updatedQuotes = newQuotes.map(
        function(newQuote, index, array) {
          if (existingQuotesIndex.hasOwnProperty(newQuote.symbol)) {
            // Quotes has existing entry - get last price record
            var matchingQuote = existingQuotesIndex[newQuote.symbol];
            var lastPrice = matchingQuote.prices[matchingQuote.prices.length - 1];
            if ((newQuote.timestamp != lastPrice.timestamp) || (newQuote.price != lastPrice.price)) {
              // New quote is a change so add price record
              matchingQuote.prices.push({timestamp: newQuote.timestamp, price: newQuote.price});
            }
            existingQuotesIndex[newQuote.symbol] = null; // Bad side effect
            return matchingQuote;
          }
          // Has no existing record, so create new quote record
          return {symbol: newQuote.symbol, prices: [{timestamp: newQuote.timestamp, price: newQuote.price}]};
        });
      // For any existing quotes that did not have matching new quotes add those records
      for (var existingQuoteSymbol in existingQuotesIndex) {
        var existingQuote = existingQuotesIndex[existingQuoteSymbol];
        if (existingQuote != null) { updatedQuotes.push(existingQuote); }
      }
      this.setState({quotes: updatedQuotes});
    },
    getInitialQuotes: function() {
      var url = this.props.protocol + '//' + this.props.host + '/api/quotes';
      this.props.ajax.getJsonData(
        url,
        function(quotes) { this.mergeNewQuotesUpdatingState(quotes); }.bind(this),
        function(xhr)    { console.log('getInitialQuotes error status : ' + xhr.status); });
    },
    subscribeForQuoteUpdates: function() {
      this.props.socket.registerMessageHandler(
        'quote-updates',
        function(data) { this.mergeNewQuotesUpdatingState(data.quotes); }.bind(this));
    },
    getInitialState: function() {
      return {quotes: []};
    },
    componentDidMount: function() {
      this.getInitialQuotes();
      this.subscribeForQuoteUpdates();
    },
    render: function() {
      return (
        <div className='stocks'>
          <h1>Quotes</h1>
          <AddStock protocol={this.props.protocol} host={this.props.host} currencies={this.props.currencies} ajax={ajax} />
          <Quotes quotes={this.state.quotes} />
          <pre>{JSON.stringify(this.state, null, 2)}</pre>
        </div>
      );
    }
  });
  
  var AddStock = React.createClass({
    onFormChange: function(fieldName, event) {
      // Will rebuild if applicable
      var error = '';
      // Existing state
      var newSymbol = this.state.symbol;
      var newCurrency = this.state.currency;
      // Process proposed change
      if (fieldName === 'symbol') {
        newSymbol = event.target.value.trim().toUpperCase();
        // Pending - Do some validation
        if (newSymbol.length > 0 && newSymbol.length < 4) {
          error = 'Symbol must be at least 4 chars long';
        }
      }
      else if (fieldName === 'currency') {
        newCurrency = event.target.value.trim().toUpperCase();
        // Pending - Do some validation
      }

      // Determine if submission allowed - content to submit exists and no error
      var allowSubmission = (((newSymbol + newCurrency).length > 0) && (error.length === 0));
      this.setState({symbol: newSymbol, currency: newCurrency, allowSubmission: allowSubmission, error: error});
    },
    onFieldChange: function(fieldName, fn) {
      return function(event) { fn(fieldName, event); }
    },
    handleSubmit: function(e) {
      e.preventDefault();
      this.setState({allowSubmission: false, isBeingSaved: true});
      var url = this.props.protocol + '//' + this.props.host + '/api/stocks';
      var data = {symbol: this.state.symbol, currency: this.state.currency};
      this.props.ajax.postJsonData(
        url,
        data,
        function(result) { this.setState(this.getInitialState()); }.bind(this),
        function(xhr)    { this.setState({allowSubmission: true, isBeingSaved: false, error: 'Save error : ' + xhr.status}); }.bind(this));
    },
    getInitialState: function() {
      return {symbol: '', currency: '', allowSubmission: false, isBeingSaved: false, error: ''};
    },
    render: function() {
      var currencyNodes = this.props.currencies
        .map(function(currency, index) { return (<option value={currency} key={index}>{currency}</option>); });
      return (
        <div className='add-stock'>
          <form onSubmit={this.handleSubmit}>
            <input type='text' placeholder='Symbol' value={this.state.symbol} onChange={this.onFieldChange('symbol', this.onFormChange)} />
            <select placeholder='Currency' value={this.state.currency} onChange={this.onFieldChange('currency', this.onFormChange)}>
              {currencyNodes}
            </select>
            <input className='add-stock-button' type='submit' value='Add' disabled={!this.state.allowSubmission} />
          </form>
          <image className={'saving-image ' + this.state.isBeingSaved} src='/images/ajax-loader.gif' />
          <span className='error'>{this.state.error}</span>
          <pre>{JSON.stringify(this.state, null, 2)}</pre>
        </div>
      );
    }
  });
  
  var Quotes = React.createClass({
    render: function() {
      var quoteNodes = this.props.quotes
        .sort(function(quote1, quote2) { return quote1.symbol.localeCompare(quote2.symbol); })
        .map(function(quote, index) { return (<Quote quote={quote} key={index} />); });
      return (
        <div className='quotes'>
          <div className='quote-list'>
            <div className='quote-entry header'>
              <div className='quote symbol'>Symbol</div>
              <div className='quote timestamp'>As of</div>
              <div className='quote price'>Price</div>
            </div>
            {quoteNodes}
          </div>
        </div>
      );
    }
  });
  
  var Quote = React.createClass({
    determineLocaleDisplayTimestamp: function(quote) {
      var timestamp = new Date(quote.timestamp);
      var now = new Date();
      if (timestamp.getUTCDate() != now.getUTCDate()) {
        return timestamp.toLocaleString();
      }
      return timestamp.toLocaleTimeString();
    },
    determinePriceMovementLabel: function(quote) {
      if (quote.prices.length < 2) { return 'unchanged'; }
      var movementValue = quote.prices[quote.prices.length - 1].price - quote.prices[quote.prices.length - 2].price;
      if (movementValue < 0) { return 'down'; }
      if (movementValue === 0) { return 'unchanged'; }
      return 'up';
    },
    render: function() {
      var quote = this.props.quote;
      var latestQuotePrice = quote.prices[quote.prices.length - 1];
      var localeTimestamp = this.determineLocaleDisplayTimestamp(latestQuotePrice);
      var priceMovementLabel = this.determinePriceMovementLabel(quote);
      return (
        <div className='quote-entry'>
          <div className='quote symbol'>
            {quote.symbol}
          </div>
          <div className='quote timestamp'>
            {localeTimestamp}
          </div>
          <div className={'quote price ' + priceMovementLabel}>
            {latestQuotePrice.price}
          </div>
        </div>
      );
    }
  });

  var Currencies = React.createClass({
    getInitialState: function() {
      return {};
    },
    componentDidMount: function() {
    },
    render: function() {
      return (
        <div className='currencies'>
          <h1>Currencies</h1>
        </div>
      );
    }
  });

  React.render(
    <App protocol={window.location.protocol} host={window.location.host} ajax={ajax} socket={socket} />,
    document.getElementById('content')
  );
})();
