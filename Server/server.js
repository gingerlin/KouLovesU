var bodyParser = require('body-parser');
var express = require('express');
var http = require('http');
var https = require('https');
var app = express();
var server = http.createServer(app);
var fs = require('fs');

app.use(bodyParser());

app.post('/registerGCMId', function (request, response) {
    var reg_id = request.body.reg_id;
    if (reg_id != undefined) {
        fs.appendFile(__dirname + '/regIds.txt', '\n' + reg_id, function (err) {
            if (err) {
                response.send("FL " + err);
            } else {
                response.send("SC");
            }
        });
    } else {
        response.send("FL Cannot get reg_id");
    }
});

app.post('/broadcastNotification', function (request, response) {

    var type = request.body.type;
    var message = request.body.message;
    var title = request.body.title;
    var password = request.body.password;
    var latestVersion = request.body.latestVersion;

    if (password == 'lucherlovesu') {
        broadcastNotification(type, message, title, latestVersion);
        response.send("Message Submitted");
    } else {
        response.send("Password Incorret");
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
        sendNotification(type, regIds, message, title, latestVersion);
        response.send("Message Submitted");
    } else {
        response.send("Password Incorret");
    }

});

server.listen(85, function () {
});

function broadcastNotification(type, message, title, latestVersion) {

    fs.readFile(__dirname + '/regIds.txt', function (err, data) {
        if (err) {
            console.log("an error occurred while reading regIds.txt")
        } else {
            regIds = data.toString().split('\n');

            sendNotification(type, regIds, message, title, latestVersion);
        }
    });
}

function sendNotification(type, regIds, message, title, latestVersion) {
    if (type == undefined)
        type = 0;
    if (regIds == undefined)
        return;
    if (type == 1 && latestVersion == undefined)
        return;

    regIds = '[' + regIds + ']';
    regIds = JSON.parse(regIds);

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
            console.log(chunk);
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