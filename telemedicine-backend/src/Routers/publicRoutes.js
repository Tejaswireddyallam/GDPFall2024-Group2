const express = require("express");
const router = express.Router();
const publicControllers = require('../Controllers/publicControllers.js');


router.get('/test', (req,res) => {
    res.send("server running...")
});

router.post('/addActivityLog', publicControllers.addActivityLog);

module.exports = router;