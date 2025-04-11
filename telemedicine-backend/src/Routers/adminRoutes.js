const express = require("express");
const router = express.Router();
const adminController = require('../Controllers/adminControllers.js');


const renderAdminLoginPage = (req, res) => {
    res.sendFile(path.join(__dirname, "../views/admin/index.html"));
};

router.get('/test', (req,res) => {
    res.send("server running...")
});



router.get('/activitylogs', adminController.getAllActivityLogs);
router.post('/login', adminController.adminLogin);
router.put('/activitylogs/:logId/status', adminController.updateActivityStatus);


module.exports = router;