# findash
This is a mickey mouse application to display financial information, it provides no great value other than being used to learn some clojure and having discussions on the same with some collegues that have been asking me about clojure.
Be warned there could be some pretty poor ideas here.

The following are some of the features

- Most of this could be done on the browser as all data is retrieved from public apis and no server side storage used.
- Uses core.async for pub\sub of data updates.
- Uses Facebooks react for the client page.
- Uses web sockets for streaming updates to clients.
- Uses ajax for some data retrieval.

## Outstanding issues I hope to address to flesh out my understanding
- Can I create a watcher abstraction.
- Figure out server side error handling, i.e. doesn't cater for failed data retrieval, can I use a clojure circuit breaker ? 
- Figure out deployment issues - profiles, environments, ubjerjar etc.
- Deploy on heroku.
- Use Stuart Sierra's component - Still trying to grok state in a clojure app.
- Switch all client interactions to just using web sockets alone.
- Switch to using clojurescript with om.
- Make app client aware - So server only sends data that the client requires, this will require the storage of client selections.
- Use cookies for sessions and user preferences so user does not need to rebuild when they revisit.
- Authentiaction\Authorisation.
- Display graphs with d3.js or dimplejs. Currently storing all streamed updates in state so data is available.
- ....

