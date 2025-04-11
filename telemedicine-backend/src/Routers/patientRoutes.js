const express = require("express");
const router = express.Router();
const dotenv = require('dotenv').config();
const Controllers = require("../Controllers/patientControllers.js");
const multer = require("multer");
const path = require('path');
const time = new Date();
const cloudinary = require('cloudinary').v2;
const { CloudinaryStorage } = require("multer-storage-cloudinary");
const rateLimit = require("express-rate-limit");

cloudinary.config({
    cloud_name: process.env.CLOUDINARY_NAME,
    api_key: process.env.CLOUDINARY_API_KEY,
    api_secret: process.env.CLOUDINARY_API_SECRET
});
    
const storage = new CloudinaryStorage({
    cloudinary: cloudinary,
    params: {
    folder: "Patients"
    }
});

const upload = multer({ storage: storage });

const recentRequests = new Map();

const preventDuplicateRequests = (req, res, next) => {
    const requestKey = `${req.method}-${req.originalUrl}-${JSON.stringify(req.body || {})}`;

    const currentTime = Date.now();
    if (recentRequests.has(requestKey)) {
        const lastRequestTime = recentRequests.get(requestKey);
        if (currentTime - lastRequestTime < 2000) {
            return res.status(429).json({ success: false, error: "Duplicate request detected. Please wait a moment." });
        }
    }

    recentRequests.set(requestKey, currentTime);

    setTimeout(() => recentRequests.delete(requestKey), 2000);

    next();
};


router.use(preventDuplicateRequests);

const renderResetPasswordPage = (req, res) => {
    res.sendFile(path.join(__dirname, "../views/resetPassword.html"));
};

// GET Requets
router.get('/test', (req,res) => {
    res.send("patient server running...")
})
router.get("/appointment/:patientID", Controllers.getAppointmentsByPatient);
router.get("/search-doctors", Controllers.searchDoctors);
router.get("/pharmacyList", Controllers.getPharmacies);
router.get("/medicalRecords/:patientID", Controllers.getMedicalRecord);
//router.get("/:recordID/:patientID", Controllers.getMedicalRecord);
router.get('/verify-email', Controllers.verifyEmail);
router.get('/getDoctors', Controllers.getDoctorsList);
router.get("/getDoctor/:doctorId", Controllers.getDoctor);
router.get("/reset-password", renderResetPasswordPage); // Serves the HTML page

// POST Requests
router.post('/signup', Controllers.signup);
router.post('/login', Controllers.login);
//router.post('/getAppointment', Controllers)
router.post("/schedule", Controllers.scheduleAppointment);
router.post("/validate", Controllers.validate);
router.post("/fetchProfile", Controllers.fetchProfile);
router.post("/forgot-password", Controllers.forgotPassword);
router.post("/reset-password", Controllers.resetPassword);
router.post('/uploadMedicalRecord', Controllers.uploadMedicalRecord);


// PUT Requests
router.put("/appointment/update-status", Controllers.updateAppointmentStatus);
router.put('/updateProfile', Controllers.updateProfile);
router.put('/appointment/reschedule', Controllers.rescheduleAppointment);

// DELETE Requests
router.delete("/delete/:appointmentID", Controllers.deleteAppointment);
router.post('/deleteMedicalRecord', Controllers.deleteMedicalRecord);

module.exports = router;