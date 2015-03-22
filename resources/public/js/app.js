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
      console.log("socket onmessage : Received " + event.data);
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
  render: function() {
    return (
      <div className="quote">
        <div className="symbol">
          {this.props.symbol}
        </div>
        <div className="price">
          {this.props.price}
        </div>
      </div>
    );
  }
});

var QuoteList = React.createClass({
  render: function() {
    var quoteNodes = this.props.quotes.map(function(quote, index) {
      return (
        <Quote symbol={quote.symbol} price={quote.price} key={index} />
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
    var proposedQuotesIndex = newQuotes.reduce(
      function(accum, current, index, array) { accum[current.symbol] = index; return accum; }, 
      {});
    for (var index = 0; index < this.state.quotes.length; index++) {
      var existingQuote = this.state.quotes[index];
      if (!proposedQuotesIndex.hasOwnProperty(existingQuote.symbol)) {
        newQuotes.push(existingQuote);
      }
    }
    var state = this.state; state.quotes = newQuotes; // We mutate so we do not overwrite any other state (None here at this time)
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
    return {quotes: []};
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
