const http = require("http");
const app = require("./src/app.js");
const dotenv = require("dotenv").config();
const port = process.env.PORT || 4000;
const server = http.createServer(app);

server.listen(port, () => {
  console.log(`local server started on http://localhost:${port}`);
});
