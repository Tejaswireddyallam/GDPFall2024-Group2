const express = require("express");
const router = express.Router();
const Controllers = require("../Controllers/doctorControllers.js")
const path = require('path');

const renderResetPasswordPage = (req, res) => {
    res.sendFile(path.join(__dirname, "../views/resetPassword.html"));
};

// GET Requets
router.get('/test', (req,res) => {
    res.send("server running...")
});

router.get("/appointment/:doctorID", Controllers.getAppointmentsByDoctor);
router.get("/appointment/details/:appointmentID", Controllers.getAppointmentDetails);
router.get("/getPatient/:patientId", Controllers.getPatient);
router.get("/:recordID/:patientID", Controllers.getMedicalRecord);
router.get('/verify-email', Controllers.verifyEmail);
router.get('/getPatients', Controllers.getPatients);
router.get("/reset-password", renderResetPasswordPage); // Serves the HTML page

// POST Requests
router.post('/signup', Controllers.signup);
router.post('/login', Controllers.login);
router.post("/create", Controllers.createMedicalRecord);
router.post("/validate", Controllers.validate);
router.post("/fetchProfile", Controllers.fetchProfile);
router.post("/forgot-password", Controllers.forgotPassword);
router.post("/reset-password", Controllers.resetPassword);


// PUT Requests
router.put("/appointment/update-status", Controllers.updateAppointmentStatusByDoctor);
router.put('/updateProfile', Controllers.updateProfile);

// DELETE Requests
router.delete("/appointment/cancel/:appointmentID", Controllers.cancelAppointmentByDoctor);

module.exports = router;