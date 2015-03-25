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
        socket = null;
        setTimeout(function() { this.open(url, quotesUpdatedHandlerFn); }.bind(this), this.onDisconnectedRetryIntervalInMs);
      }.bind(this);
    },
    registerMessageHandler(messageType, handlerFn) {
      // Only one per message type
      this.messageHandlers[messageType] = handlerFn;
    }
  };

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
      var priceMovementClassName = 'price ' + this.determinePriceMovementLabel(quote);
      return (
        <div className='quote'>
          <div className='symbol'>
            {quote.symbol}
          </div>
          <div className='timestamp'>
            {localeTimestamp}
          </div>
          <div className={priceMovementClassName}>
            {latestQuotePrice.price}
          </div>
        </div>
      );
    }
  });

  var QuoteList = React.createClass({
    render: function() {
      var quoteNodes = this.props.quotes
        .sort(function(quote1, quote2) { return quote1.symbol.localeCompare(quote2.symbol); })
        .map(function(quote, index) { return (<Quote quote={quote} key={index} />);
      });
      return (
        <div className='quoteList'>
          {quoteNodes}
        </div>
      );
    }
  });

  var AddStockForm = React.createClass({
    onChange: function(fieldName, event) {
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
      if (fieldName === 'currency') {
        newCurrency = event.target.value.trim().toUpperCase();
        // Pending - Do some validation
      }
      // Determine if submission allowed - content to submit exists and no error
      var allowSubmission = (((newSymbol + newCurrency).length > 0) && (error.length === 0));
      this.setState({symbol: newSymbol, currency: newCurrency, allowSubmission: allowSubmission, error: error});
    },
    onSymbolChange: function(event) {
      this.onChange('symbol', event);
    },
    onCurrencyChange: function(event) {
      this.onChange('currency', event);
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
      return (
        // TEMP DIV so we can see the state, JSX requires a wrapper
        <div>
          <form className='addStockForm' onSubmit={this.handleSubmit}>
            <input type='text' placeholder='Symbol' value={this.state.symbol} onChange={this.onSymbolChange} />
            <input type='text' placeholder='Currency' value={this.state.currency} onChange={this.onCurrencyChange} />
            <input className='addStock' type='submit' value='Add' disabled={!this.state.allowSubmission} />
          </form>
          <image className={'addStockWorkingImage ' + this.state.isBeingSaved} src='/images/ajax-loader.gif' />
          <div>{this.state.error}</div>
          <pre>{JSON.stringify(this.state, null, 2)}</pre>
        </div>
      );
    }
  });

  var App = React.createClass({
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
    runQuotesUpdateListener: function() {
      var url = 'ws://' + this.props.host + '/ws';
      this.props.socket.open(url);
      this.props.socket.registerMessageHandler(
        'quote-updates',
        function(data) { this.mergeNewQuotesUpdatingState(data.quotes); }.bind(this));
    },
    getInitialState: function() {
      return {quotes: []};
    },
    componentDidMount: function() {
      this.getInitialQuotes();
      this.runQuotesUpdateListener();
    },
    render: function() {
      return (
        <div className='stocks'>
          <AddStockForm protocol={this.props.protocol} host={this.props.host} ajax={ajax} />
          <div className='quotes'>
            <h1>Quotes</h1>
            <QuoteList quotes={this.state.quotes} />
          </div>
          <pre>{JSON.stringify(this.state, null, 2)}</pre>
        </div>
      );
    }
  });

  React.render(
    <App protocol={window.location.protocol} host={window.location.host} ajax={ajax} socket={socket} />,
    document.getElementById('content')
  );
})();
