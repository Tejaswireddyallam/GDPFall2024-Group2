const jwt = require("jsonwebtoken");
const bcrypt = require('bcrypt');
const pool = require("../pool");
const path = require('path');

var savedOTPS = {
};

const signup = async (req, res) => {
    try {
        const { username, email, password } = req.body;
        if (!username || !email || !password) {
            return res.status(400).json({ message: 'Please provide all required fields.' });
        }

        const [existingUser] = await pool.execute('SELECT * FROM users WHERE email = ?', [email]);
        if (existingUser.length > 0) {
            return res.status(409).json({ message: 'Email is already registered.' });
        }

        const hashedPassword = await bcrypt.hash(password, 10);
        const insertQuery = 'INSERT INTO users (fullname, email, password, ) VALUES (?, ?, ?)';
        const [result] = await pool.execute(insertQuery, [username, email, hashedPassword]);

        if (result.affectedRows > 0) {
            return res.status(201).json({ message: 'User registered successfully.' });
        } else {
            return res.status(500).json({ message: 'Failed to register user.' });
        }
    } catch (error) {
        console.error('Error during signup:', error.message);
        res.status(500).json({ message: 'An error occurred during signup.' });
    }
};

const login = async (req, res) => {
    try {
        const { email, password } = req.body;
        if (!email || !password) {
            return res.status(400).json({ message: 'Please provide both email and password.' });
        }

        const [user] = await pool.execute('SELECT * FROM users WHERE email = ?', [email]);
        if (user.length === 0) {
            return res.status(401).json({ message: 'Invalid email or password.' });
        }

        const foundUser = user[0];
        const isPasswordValid = await bcrypt.compare(password, foundUser.password_hash);
        if (!isPasswordValid) {
            return res.status(401).json({ message: 'Invalid email or password.' });
        }

        const tokenPayload = { id: foundUser.id, username: foundUser.username };
        const token = jwt.sign(tokenPayload, config.JWT_SECRET, { expiresIn: '7d' });

        return res.status(200).json({
        message: 'Login successful.',
        token,
        user: { id: foundUser.id, username: foundUser.username, email: foundUser.email },
        });

    } catch (error) {
        console.error('Error during login:', error.message);
        res.status(500).json({ message: 'An error occurred during login.' });
    }
};

module.exports = { signup, login };