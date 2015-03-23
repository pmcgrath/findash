var ajaxHelper = {
  getJsonData: function(url, successFn, errorFn) {
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
    }
    xhr.open("GET", url);
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.send();
  }
};

var socketHelper = {
  start: function(url, quotesUpdatedHandlerFn) {
    socket = new WebSocket(url);
    socket.onerror = function(error) {
      console.log("socket onerror :" + error);
    };
    socket.onopen = function(event) {
      console.log("socket onopen : Connected to " + event.currentTarget.url);
    };
    socket.onmessage = function(event) {
      console.log("socket onmessage : Received data");
      var data = JSON.parse(event.data);
      switch(data.messageType) {
        case "quote-updates":
          quotesUpdatedHandlerFn(data.quotes);
          break;
        default:
          console.log("No handler for message-type");
      }
    };
    socket.onclose = function(event) {
      console.log("socket onclose : Disconnected: " + event.code + " " + event.reason);
      socket = null;
    };
  }
};

var Quote = React.createClass({
  determinePriceMovementLabel: function(quote) {
    if (quote.prices.length < 2) { return "unchanged"; }
    var movementValue = quote.prices[quote.prices.length - 1].price - quote.prices[quote.prices.length - 2].price;
    if (movementValue < 0) { return "down"; }
    if (movementValue === 0) { return "unchanged"; }
    return "up";
  },
  render: function() {
    var quote = this.props.quote;
    var latestQuotePrice = quote.prices[quote.prices.length - 1];
    var priceMovementClassName = "price " + this.determinePriceMovementLabel(quote);
    return (
      <div className="quote">
        <div className="symbol">
          {quote.symbol}
        </div>
        <div className="timestamp">
          {latestQuotePrice.timestamp}
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
    var quoteNodes = this.props.quotes.map(function(quote, index) {
      return (
        <Quote quote={quote} key={index} />
      );
    });
    return (
      <div className="quoteList">
        {quoteNodes}
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

    var state = this.state; state.quotes = updatedQuotes; // We mutate so we do not overwrite any other state (None here at this time)
    this.setState(state);
  },
  getInitialQuotes: function() {
    var url = this.props.protocol + "//" + this.props.host + "/api/quotes";
    this.props.ajaxHelper.getJsonData(
      url, 
      function(quotes) { this.mergeNewQuotesUpdatingState(quotes); }.bind(this),
      function(xhr)    { console.log("getInitialQuotes error status : " + xhr.status); });
  },
  runQuotesUpdateListener: function() {
    var url = "ws://" + this.props.host + "/ws";
    this.props.socketHelper.start(
      url,
      function(quotes) { this.mergeNewQuotesUpdatingState(quotes); }.bind(this)); 
  },
  getInitialState: function() {
    //return {quotes: []};
    return {quotes: [{symbol: "APPL", prices: [{timestamp: "start", price: 1.000001}]}]};
  },
  componentDidMount: function() {
    this.getInitialQuotes();
    this.runQuotesUpdateListener();
  },
  render: function() {
    return (
      <div className="quotes">
        <h1>Quotes</h1>
        <QuoteList quotes={this.state.quotes} />
      </div>
    );
  }
});

React.render(
  <App protocol={window.location.protocol} host={window.location.host} ajaxHelper={ajaxHelper} socketHelper={socketHelper} />,
  document.getElementById('content')
);
