const mysql = require("mysql2/promise");
const fs = require("fs")
const dotenv = require("dotenv").config();

const pool = mysql.createPool({
    host: process.env.DB_HOST,
    port: process.env.DB_PORT,
    user: process.env.DB_USERNAME,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_DATABASE,
    connectionLimit: 15,
    multipleStatements: true,
    waitForConnections: true,
    queueLimit: 0,
    ssl: {
        minVersion: 'TLSv1.2', 
        ca: fs.readFileSync('src/configs/isrgrootx1.pem')
      }
});

module.exports = pool;


