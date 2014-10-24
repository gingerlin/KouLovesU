var bodyParser = require('body-parser');
var express = require('express');
var http = require('http');
var https = require('https');
var mysql = require('mysql');
var fs = require('fs');

var app = express();
var server = http.createServer(app);
var pool = mysql.createPool({
    host: 'localhost',
    user: 'koulovesu',
    password: 'PJKXdw7rX2wDMPEF',
    database: 'koulovesu',
    port: '3306'
});

var logger = require('tracer').console({
    format: "{{timestamp}} <{{title}}> {{message}} ({{file}}:{{line}})",
    dateformat: "yyyy-mm-dd' 'HH:MM:ss",
    transport: function (data) {
        console.log(data.output);
        fs.open(__dirname + '/server.log', 'a', 0666, function (e, id) {
            fs.write(id, data.output + "\n", null, 'utf8', function () {
                fs.close(id, function () {
                    if (data.output.indexOf("uncaughtException") != -1) process.exit(1);
                });
            });
        });
    }
});

var latestVersionNumber = 7;

app.use(bodyParser());

server.listen(85, function () {
    logger.log("Server Start");
});

app.use(function (request, response, next) {
    var ip = request.headers['x-forwarded-for'] || request.connection.remoteAddress;
    logger.log(ip + " : " + request.url);
    if (Object.keys(request.body).length != 0) {
        logger.log(request.body);
    }
    next();
});

app.get('/getLatestVersionNumber', function (request, response) {
    var result = { 'success': true, "latest_app_version": latestVersionNumber };
    response.send(JSON.stringify(result));
    logger.log(JSON.stringify(result));
});

app.get('/getSolutions', function (request, response) {
    var query = "SELECT `id`, `author_id`, `number`, `title`, `content` FROM `solutions`";
    var callback = function (result) {
        response.send(result);
        logger.log(result);
    }
    getSQL(callback, query, null);
});

app.post('/registerGCMId', function (request, response) {
    var reg_id = request.body.reg_id;
    var device_id = request.body.device_id;
    if (reg_id != undefined && device_id != undefined) {
        var query = "REPLACE INTO `regids` (`device_id`, `reg_id`) VALUES(?, ?)";
        var params = [device_id, reg_id];
        var callback = function (result) {
            response.send(result);
            logger.log(result);
        }
        getSQL(callback, query, params);
    } else {
        var result = JSON.stringify({ 'success': false, 'error': "Cannot get reg_id and device_id" });
        response.send(result);
        logger.log(result);
    }
});

app.post('/broadcastNotification', function (request, response) {

    var type = request.body.type;
    var message = request.body.message;
    var title = request.body.title;
    var password = request.body.password;
    var latestVersion = request.body.latestVersion;

    if (password == 'lucherlovesu') {

        var query = "SELECT `reg_id` FROM `regids`";
        var callback = function (result) {
            if (result['success'] === true) {
                var regIds = [];
                for (var i = 0; i < result['rows'].length; i++) {
                    regIds.push(result['rows'][i]['reg_id']);
                }
                sendNotification(type, regIds, message, title, latestVersion, function (chunk) {
                    result.chunk = chunk;
                    response.send(JSON.stringify(result));
                    logger.log(JSON.stringify(result));
                });
            } else {
                response.send(JSON.stringify(result));
                logger.log(JSON.stringify(result));
            }
        }
        getSQL(callback, query, null);

    } else {
        var result = JSON.stringify({ 'success': false, 'error': "Password Incorret" });
        response.send(result);
        logger.log(result);
    }

});

app.post('/sendNotification', function (request, response) {

    var type = request.body.type;
    var regIds = request.body.regIds;
    var message = request.body.message;
    var title = request.body.title;
    var password = request.body.password;
    var latestVersion = request.body.latestVersion;

    if (password == 'lucherlovesu') {
        sendNotification(type, regIds.split(','), message, title, latestVersion, function (chunk) {
            result = {
                'success': true,
                'chunk' : chunk
            };
            response.send(JSON.stringify(result));
            logger.log(JSON.stringify(result));
        });
    } else {
        var result = JSON.stringify({ 'success': false, 'error': "Password Incorret" });
        response.send(result);
        logger.log(result);
    }

});

function sendNotification(type, regIds, message, title, latestVersion, callback) {
    if (type == undefined)
        type = 0;
    if (regIds == undefined)
        return;
    if (type == 1 && latestVersion == undefined)
        return;

    if (Object.prototype.toString.call(regIds) === '[object Array]') {
        
    } else {
        regIds = '[' + regIds + ']';
        regIds = JSON.parse(regIds);
    }

    var headers = {
        Authorization: "key=AIzaSyDs-3zkcb-kTlq-HNIfrwVv0WoloXHUKj8",
        "Content-Type": "application/json"
    };

    var options = {
        hostname: "android.googleapis.com",
        port: 443,
        path: "/gcm/send",
        method: "POST",
        headers: headers
    };

    var require = https.request(options, function (response) {
        response.setEncoding("utf8");
        response.on("data", function (chunk) {
            callback(chunk);
        });
    });

    var data = {
        "registration_ids": regIds,
        data: { type: type }
    };

    if (title != undefined) {
        data.data.title = title;
    }
    if (message != undefined) {
        data.data.message = message;
    }
    if (latestVersion != undefined) {
        data.data.latestVersion = latestVersion;
    }

    require.on("error", function (e) {
        console.error("Error: " + e.message);
    });

    require.write(JSON.stringify(data));

    require.end();
}

function getSQL(callback, queryString, params) {

    pool.getConnection(function (err, connection) {
        connection.query(queryString, params, function (err, rows) {
            var result = {};
            if (err) {
                result['success'] = false;
                result['error'] = err;
                console.error(err);
            } else {
                result['success'] = true;
                if (rows) {
                    result['rows'] = rows;
                }
            }
            callback(result)
            connection.release();
        });
    });
};