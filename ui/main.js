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
    drawCurrentPlayer();
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

function drawCurrentPlayer() {
    context.beginPath();
    context.fillStyle = player.color
    context.arc(player.locX, player.locY, player.radius, 0, Math.PI * 2);
    context.fill();
    context.lineWidth = 3;
    context.strokeStyle = 'rgb(0,255,0)';
    context.stroke();
}

function drawPlayers() {
    players.filter(x => x.uid !== player.uid).forEach((p) => {
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

function updatePlayerScore() {
    $('.player-score').text(player.score || 0);
}

function updateLeaderBoard() {
    $('.leader-board').empty();
    players.sort((a, b) => b.score - a.score).forEach(player => {
        const playerItem = $('<li class="leaderboard-player">').text(`${player.playerName}: ${player.score}`);
        $('.leader-board').append(playerItem);
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
    switch (message && message._type) {
        case 'InitMessageResponse':
            orbs = message.data.orbs
            player = {...player, ...message.data.playerData}
            setInterval(() => {
                if(player.uid && player.xVector) socket.send(JSON.stringify({
                    _type: 'TickMessage',
                    data: {
                        uid: player.uid,
                        xVector: player.xVector || 0,
                        yVector: player.yVector || 0
                    }
                }));
            }, 33);
            break;
        case 'PlayerListMessageResponse':
            players = message.data;
            updateLeaderBoard();
            break;
        case 'TickMessageResponse':
            player = {...player, ...message.data.playerData};
            orbs = message.data.orbs;
            updatePlayerScore();
            break;
    }
});
