// ==================================
// ============= UI =================
// ==================================

let windowHeight = $(window).height();
let windowWidth = $(window).width();

let player = {}
let players = [];
let orbs = [];

let canvas = document.querySelector('#game-board');
let context = canvas.getContext('2d');
canvas.width = windowWidth;
canvas.height = windowHeight;

$(window).on("load", function () {
    $('#game-start').modal('show')
});

$('.name-form').submit((e) => {
    e.preventDefault()
    player.name = document.querySelector('#name-input').value;
    $('#game-start').modal('hide');
    $('.modal').modal('hide');
    $('.hiddenOnStart').removeAttr('hidden');
    init();
})

// ==================================
// ============= DRAWING ============
// ==================================

function draw() {
    resetTransform();
    clearScreen();
    clampCamera();
    drawPlayers();
    drawOrbs();
    requestAnimationFrame(draw);
}

function clearScreen() {
    context.clearRect(0, 0, canvas.width, canvas.height);
}

function clampCamera() {
    const camX = -player.locX + canvas.width / 2;
    const camY = -player.locY + canvas.height / 2;
    context.translate(camX, camY);
}

function drawPlayers() {
    players.forEach((p) => {
        context.beginPath();
        context.fillStyle = p.color;
        context.arc(p.locX, p.locY, p.radius, 0, Math.PI * 2);
        context.fill();
        context.lineWidth = 3;
        context.strokeStyle = 'rgb(0,255,0)';
        context.stroke();
    });
}

function drawOrbs() {
    orbs.forEach((orb) => {
        context.beginPath();
        context.fillStyle = orb.color;
        context.arc(orb.locX, orb.locY, orb.radius, 0, Math.PI * 2);
        context.fill();
    });
}

function resetTransform() {
    context.setTransform(1, 0, 0, 1, 0, 0);
}

canvas.addEventListener('mousemove', (event) => {
    const mousePosition = {
        x: event.clientX,
        y: event.clientY
    };
    let xVector, yVector;
    const angleDeg = Math.atan2(mousePosition.y - (canvas.height / 2), mousePosition.x - (canvas.width / 2)) * 180 / Math.PI;
    if (angleDeg >= 0 && angleDeg < 90) {
        xVector = 1 - (angleDeg / 90);
        yVector = -(angleDeg / 90);
    } else if (angleDeg >= 90 && angleDeg <= 180) {
        xVector = -(angleDeg - 90) / 90;
        yVector = -(1 - ((angleDeg - 90) / 90));
    } else if (angleDeg >= -180 && angleDeg < -90) {
        xVector = (angleDeg + 90) / 90;
        yVector = (1 + ((angleDeg + 90) / 90));
    } else if (angleDeg < 0 && angleDeg >= -90) {
        xVector = (angleDeg + 90) / 90;
        yVector = (1 - ((angleDeg + 90) / 90));
    }
    player.xVector = xVector;
    player.yVector = yVector;
})

// Websocket

// ==================================
// =============WEBSOCKET============
// ==================================

const socket = new WebSocket('ws://localhost:8090');

function init() {
    draw()
    socket.send(JSON.stringify({
        _type: 'InitMessage',
        data: {
            playerName: player.name
        }
    }));
}


socket.addEventListener('message', (event) => {
    const message = JSON.parse(event.data);
    switch (message.messageType) {
        case 'initReturn':
            orbs = message.data.orbs
            player = {...player, ...message.data.playerData}
            setInterval(() => {
                if(player.uid) socket.send(JSON.stringify({
                    _type: 'TickMessage',
                    data: {
                        uid: player.uid,
                        xVector: player.xVector || 0,
                        yVector: player.yVector || 0
                    }
                }));
            }, 33);
            break;
        case 'tock':
            // console.log(message.data)
            players = message.data; //.players;
            break;
        case 'tickTock':
            // console.log(message.data)
            player.locX = message.data.playerX;
            player.locY = message.data.playerY;
            break;

    }
});
