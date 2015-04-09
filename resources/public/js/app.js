/*
  See
    http://facebook.github.io/react/docs/tutorial.html

  Props validation
    Not using propTypes for validation as we will be using the minified version of react which will not write console warnings
    Could use  componentWillReceiveProps lifecycle function where the first parameter is the props object, could validate each member

*/
'use strict';

(function() {

  /* Make this a mixin ? */
  var ajax = {
    makeJsonRequest: function(url, method, data, successFn, errorFn) {
      var xhr = new XMLHttpRequest();
      xhr.onreadystatechange = function() {
        if (xhr.readyState === 4) {
          if (xhr.status === 200) {
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
    },

    createForBaseUrl: function(baseUrl) {
      // Returns a wrapper so the client does not neet to remember the base url
      return {
        getJsonData: function(url, successFn, errorFn) {
          this.getJsonData(baseUrl + url, successFn, errorFn);
        }.bind(this),

        postJsonData: function(url, data, successFn, errorFn) {
          this.postJsonData(baseUrl + url, successFn, errorFn);
        }.bind(this)
      }
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

    registerMessageHandler: function(messageType, handlerFn) {
      // Only one per message type
      console.log("Registering message handler for " + messageType);
      this.messageHandlers[messageType] = handlerFn;
    },

    deregisterMessageHandler: function(messageType) {
      // Only one per message type
      console.log("DeRegistering message handler for " + messageType);
      this.messageHandlers[messageType] = null;
    }
  };

  var App = React.createClass({
    getAjaxForBaseUrl: function() {
      return this.props.ajax.createForBaseUrl(this.props.baseUrl);
    },

    openSocket: function() {
      this.props.socket.open(this.props.socketUrl);
    },

    getCurrenciesList: function() {
      this.state.ajax.getJsonData(
        'api/currencies',
        function(currencies) { this.setState({currencies: currencies}); }.bind(this),
        function(xhr)        { console.log('getCurrenciesList error status : ' + xhr.status); });
    },
  
    getInitialState: function() {
      var ajaxForBaseUrl = this.getAjaxForBaseUrl(); // We need to do this before render so children components can use
      return {currencies: [], ajax: ajaxForBaseUrl};
    },

    getDefaultProps: function() {
      // What if being served from an app ? i.e. http://localhost/theapp
      var baseUrl = window.location.protocol + '//' + window.location.host + '/';
      var socketUrl = baseUrl.replace('http', 'ws') + 'ws'; // Takes care of http->ws and https->wss 
      return {
        displayState: false,
        baseUrl: baseUrl,
        socketUrl: socketUrl,
      };
    },

    componentDidMount: function() {
      this.openSocket();
      this.getCurrenciesList();
    },

    render: function() {
      return (
        <div className="app">
          <Stocks currencies={this.state.currencies} displayState={this.props.displayState} ajax={this.state.ajax} socket={this.props.socket} />
          <Currencies currencies={this.state.currencies} displayState={this.props.displayState} ajax={this.state.ajax} socket={this.props.socket} />
        </div>
      );
    }
  });

  var Stocks = React.createClass({
    mergeNewQuotesUpdatingState: function(newQuotes) {
      // Cater for no new quotes
      if (newQuotes === null || newQuotes.length === 0) {
        // Do nothing 
        return;
      }
      // Create map of existing quotes - key is symbol and value is the existing quote
      var existingQuotesIndex = this.state.quotes.reduce(
        function(accum, quote, index, array) { accum[quote.symbol] = quote; return accum; },
        {});
      // Map the new quotes to a new collection
      // Will take care of both quotes with existing data and previously unseen quotes
      // We maintain an array of prices for each symbol
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
      this.props.ajax.getJsonData(
        'api/quotes/latest',
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

    componentWillUnmount: function() {
      this.props.socket.deregisterMessageHandler('quote-updates');
    },

    render: function() {
      var displayState = (this.props.displayState) ? JSON.stringify(this.state, null, 2) : "";
      return (
        <div className='stocks'>
          <h1>Quotes</h1>
          <AddStock currencies={this.props.currencies} displayState={this.props.displayState} ajax={ajax} />
          <Quotes quotes={this.state.quotes} />
          <pre>{displayState}</pre>
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
      var allowSubmission = ((newSymbol != '' && newCurrency != '') && (error.length === 0));
      this.setState({symbol: newSymbol, currency: newCurrency, allowSubmission: allowSubmission, error: error});
    },

    onFieldChange: function(fieldName, fn) {
      return function(event) { fn(fieldName, event); }
    },

    handleSubmit: function(e) {
      e.preventDefault();
      this.setState({allowSubmission: false, isBeingSaved: true});
      var data = {symbol: this.state.symbol, currency: this.state.currency};
      this.props.ajax.postJsonData(
        'api/stocks',
        data,
        function(result) { this.setState(this.getInitialState()); }.bind(this),
        function(xhr)    { this.setState({allowSubmission: true, isBeingSaved: false, error: 'Save error : ' + xhr.status}); }.bind(this));
    },

    getInitialState: function() {
      return {symbol: '', currency: '', allowSubmission: false, isBeingSaved: false, error: ''};
    },

    render: function() {
      var displayState = (this.props.displayState) ? JSON.stringify(this.state, null, 2) : "";
      return (
        <div className='add-stock'>
          <form onSubmit={this.handleSubmit}>
            <input type='text' placeholder='Symbol' value={this.state.symbol} onChange={this.onFieldChange('symbol', this.onFormChange)} />
            <CurrencyDropDown currencies={this.props.currencies} value={this.state.currency} onChange={this.onFieldChange('currency', this.onFormChange)} />
            <input className='add-stock-button' type='submit' value='Add' disabled={!this.state.allowSubmission} />
          </form>
          <image className={'saving-image ' + this.state.isBeingSaved} src='/images/ajax-loader.gif' />
          &nbsp;<span className='error'>{this.state.error}</span>
          <pre>{displayState}</pre>
        </div>
      );
    }
  });

  var CurrencyDropDown = React.createClass({
    getSource: function() {
      var currenciesToAppearFirst = ['EUR', 'USD', 'GBP'];
      var currencies = this.props.currencies.filter(function(currency) { return (currenciesToAppearFirst.indexOf(currency) === -1); });
      return currenciesToAppearFirst.reduceRight(function(accum, currency) { accum.unshift(currency); return accum; }, currencies);
    },

    render: function() {
      var currencies = this.getSource();
      var currencyNodes = currencies.map(function(currency, index) { return (<option value={currency} key={index}>{currency}</option>); });
      return (
        <select placeholder='Currency' value={this.props.value} onChange={this.props.onChange}>
          <option value='' selected disabled>Select</option>
          {currencyNodes}
        </select>
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
    mergeNewRatesUpdatingState: function(newRates) {
      // Cater for no new rates
      if (newRates === null || newRates.length === 0) {
        // Do nothing 
        return;
      }
      // Create map of existing rates - key is from+to and value is the existing rate
      var existingRatesIndex = this.state.rates.reduce(
        function(accum, rate, index, array) { accum[rate.from + rate.to] = rate; return accum; },
        {});
      // Map the new rates to a new collection
      // Will take care of both rate with existing data and previously unseen rates
      // We maintain an array of rates for each currency pair
      var updatedRates = newRates.map(
        function(newRate, index, array) {
          if (existingRatesIndex.hasOwnProperty(newRate.from + newRate.to)) {
            // Rate has existing entry - get last rate record
            var matchingRate = existingRatesIndex[newRate.from + newRate.to];
            var lastRate = matchingRate.rates[matchingRate.rates.length - 1];
            if ((newRate.timestamp != lastRate.timestamp) || (newRate.rate != lastRate.rate)) {
              // New rate is a change so add rate record
              matchingRate.rates.push({timestamp: newRate.timestamp, rate: newQuote.rate});
            }
            existingRatesIndex[newRate.from + newRate.to] = null; // Bad side effect
            return matchingRate;
          }
          // Has no existing record, so create new rate record
          return {from: newRate.from, to: newRate.to, rates: [{timestamp: newRate.timestamp, rate: newRate.rate}]};
        });
      // For any existing rates that did not have matching new rate add those records
      for (var existingRateKey in existingRatesIndex) {
        var existingRate = existingRatesIndex[existingRateKey];
        if (existingRate != null) { updatedRates.push(existingRate); }
      }
      this.setState({rates: updatedRates});
    },

    getInitialRates: function() {
      this.props.ajax.getJsonData(
        'api/rates/latest',
        function(rates)	{ this.mergeNewRatesUpdatingState(rates); }.bind(this),
        function(xhr) 	{ console.log('getInitialRates error status : ' + xhr.status); });
    },

    subscribeForRateUpdates: function() {
      this.props.socket.registerMessageHandler(
        'rate-updates',
        function(data) { this.mergeNewRatesUpdatingState(data.rates); }.bind(this));
    },

    getInitialState: function() {
      return {rates: []};
    },

    componentDidMount: function() {
      this.getInitialRates();
      this.subscribeForRateUpdates();
    },

    componentWillUnmount: function() {
      this.props.socket.deregisterMessageHandler('rate-updates');
    },
    
    render: function() {
      var displayState = (this.props.displayState) ? JSON.stringify(this.state, null, 2) : "";
      return (
        <div className='currencies'>
          <h1>Currencies</h1>
          <AddRate currencies={this.props.currencies} displayState={this.props.displayState} ajax={ajax} />
          <Rates rates={this.state.rates} />
          <pre>{displayState}</pre>
        </div>
      );
    }
  });
  
  var AddRate = React.createClass({
    onFormChange: function(fieldName, event) {
      // Will rebuild if applicable
      var error = '';
      // Existing state
      var newFrom = this.state.from;
      var newTo = this.state.to;
      // Process proposed change
      if (fieldName === 'from') {
        newFrom = event.target.value;
      }
      else if (fieldName === 'to') {
        newTo = event.target.value;
      }
      if (((newFrom != '') && (newTo != '')) && (newFrom == newTo)) {
        error = 'Select a different target currency';
      }

      // Determine if submission allowed - content to submit exists
      var allowSubmission = ((newFrom != '' && newTo != '') && (error.length === 0));
      this.setState({from: newFrom, to: newTo, allowSubmission: allowSubmission, error: error});
    },

    onFieldChange: function(fieldName, fn) {
      return function(event) { fn(fieldName, event); }
    },

    handleSubmit: function(e) {
      e.preventDefault();
      this.setState({allowSubmission: false, isBeingSaved: true});
      var data = {from: this.state.from, to: this.state.to};
      this.props.ajax.postJsonData(
        'api/currencypairs',
        data,
        function(result) { this.setState(this.getInitialState()); }.bind(this),
        function(xhr)    { this.setState({allowSubmission: true, isBeingSaved: false, error: 'Save error : ' + xhr.status}); }.bind(this));
    },

    getInitialState: function() {
      return {from: '', to: '', allowSubmission: false, isBeingSaved: false, error: ''};
    },

    render: function() {
      var displayState = (this.props.displayState) ? JSON.stringify(this.state, null, 2) : "";
      return (
        <div className='add-rate'>
          <form onSubmit={this.handleSubmit}>
            <CurrencyDropDown currencies={this.props.currencies} value={this.state.from} onChange={this.onFieldChange('from', this.onFormChange)} />
            <CurrencyDropDown currencies={this.props.currencies} value={this.state.to} onChange={this.onFieldChange('to', this.onFormChange)} />
            <input className='add-rate-button' type='submit' value='Add' disabled={!this.state.allowSubmission} />
          </form>
          <image className={'saving-image ' + this.state.isBeingSaved} src='/images/ajax-loader.gif' />
          &nbsp;<span className='error'>{this.state.error}</span>
          <pre>{displayState}</pre>
        </div>
      );
    }
  });

  var Rates = React.createClass({
    render: function() {
      var rateNodes = this.props.rates
        .sort(function(rate1, rate2) { return (rate1.from + rate1.to).localeCompare((rate2.from + rate2.to)); })
        .map(function(rate, index) { return (<Rate rate={rate} key={index} />); });
      return (
        <div className='rates'>
          <div className='rate-list'>
            <div className='rate-entry header'>
              <div className='rate from'>From</div>
              <div className='rate to'>To</div>
              <div className='rate timestamp'>As of</div>
              <div className='rate therate'>Rate</div>
            </div>
            {rateNodes}
          </div>
        </div>
      );
    }
  });

  var Rate = React.createClass({
    determineLocaleDisplayTimestamp: function(rate) {
      var timestamp = new Date(rate.timestamp);
      var now = new Date();
      if (timestamp.getUTCDate() != now.getUTCDate()) {
        return timestamp.toLocaleString();
      }
      return timestamp.toLocaleTimeString();
    },

    determineRateMovementLabel: function(rate) {
      if (rate.rates.length < 2) { return 'unchanged'; }
      var movementValue = rate.rates[rate.rates.length - 1].rate - rate.rates[rate.rates.length - 2].rate;
      if (movementValue < 0) { return 'down'; }
      if (movementValue === 0) { return 'unchanged'; }
      return 'up';
    },

    render: function() {
      var rate = this.props.rate;
      var latestRate = rate.rates[rate.rates.length - 1];
      var localeTimestamp = this.determineLocaleDisplayTimestamp(latestRate);
      var priceMovementLabel = this.determineRateMovementLabel(rate);
      return (
        <div className='rate-entry'>
          <div className='rate from'>
            {rate.from}
          </div>
          <div className='rate to'>
            {rate.to}
          </div>
          <div className='rate timestamp'>
            {localeTimestamp}
          </div>
          <div className={'rate therate ' + priceMovementLabel}>
            {latestRate.rate}
          </div>
        </div>
      );
    }
  });

  React.render(
    <App displayState={false} ajax={ajax} socket={socket} />,
    document.getElementById('content')
  );
})();
