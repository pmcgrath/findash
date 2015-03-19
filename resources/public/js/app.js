var app = (function() {
  var me = {}

  function openSocket() {
    console.log("Open socket - starting");
    
    var uri = "ws://" + me.config.host + "/ws";
    socket = new WebSocket(uri);
    socket.onerror = function(error) {
      console.log("socket onerror :" + error);
    };
    socket.onopen = function(event) {
      console.log("socket onopen : Connected to " + event.currentTarget.url);
    };
    socket.onmessage = function(event) {
      console.log("socket onmessage : Received " + event.data);
      me.config.latestElement.innerHTML = event.data;
    };
    socket.onclose = function(event) {
      console.log("socket onclose : Disconnected: " + event.code + " " + event.reason);
      socket = undefined;
    };
  };

  me.start = function (config) {
    console.log("Starting start");
    me.config = config;
    openSocket();
    console.log("Completed start");
  };

  return me;
}());

window.onload = function() {
  var config = {
    host: location.host,
    latestElement: document.getElementById('latest')
  };
  app.start(config);
};
