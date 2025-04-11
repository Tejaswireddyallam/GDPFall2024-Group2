const express = require("express");
const helmet = require("helmet");
const cors = require("cors");
const app = express();
const pool = require('./configs/pool.js');
const doctorRoutes = require("./Routers/doctorRoutes.js");
const patientRoutes = require("./Routers/patientRoutes.js");
const adminRoutes = require("./Routers/adminRoutes.js");

const sendAppointmentReminders = require("../Reminder.js");

// const fileupload = require("express-fileupload");

// security
app.use(
    helmet({
      contentSecurityPolicy: false,
      xDownloadOptions: false,
    })
);

// cors
app.use(cors());
// convert everything to json
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.use("/api/doctor", doctorRoutes);
app.use("/api/patient", patientRoutes);
app.use("/api/admin", adminRoutes)


// app.use(fileupload({ useTempFiles: true }));


app.get("/", (req, res) => res.send("server running..."));
app.post("/login", (req,res) => {
  console.log(req.body)
})

sendAppointmentReminders();

process.on('SIGINT', async () => {
    try {
      await pool.end();
      console.log('Database pool closed.');
      process.exit(0);
    } catch (err) {
      console.error('Error closing database pool:', err.message);
      process.exit(1);
    }
});
  

module.exports = app;
