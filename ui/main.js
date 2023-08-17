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

const colorList = [
    "#171717", "#2E2E2E", "#454545", "#5C5C5C", "#737373", "#8A8A8A", "#5B7586", "#8CA2B0", "#36454F",
    "#BF181D", "#ED7377", "#B35F00", "#000080", "#38B261", "#96DEAE", "#E3AE09", "#FADB7C", "#635345",
    "#BEAEA1", "#362624", "#212121", "#383838", "#B7D100", "#E5FF2E", "#168900", "#ADFF9E", "#D100D1",
    "#999999", "#6B6B6B", "#DEDEDE", "#B0B0B0", "#E4AD58", "#DD9A30", "#F1D4A7", "#483C32", "#AB9786",
    "#7E6958", "#B75CFF", "#A32EFF", "#5C6380", "#DCDEE6", "#8C8C8C", "#A52217", "#EE8D85", "#EAE000",
    "#BCB400", "#FFFDD0", "#E0B0FF", "#D40032", "#FF8DA7", "#ED4002", "#197D4B", "#93E9BE", "#024376"
]
canvas.style.backgroundColor = colorList[Math.floor(Math.random()*colorList.length)]

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

$('#play-again').click((e) => {
    e.preventDefault();
    window.location.reload();
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
    drawBodyParts(player)
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
        drawBodyParts(p)
    });
}

function drawBodyParts(player) {
    for (let i = 0; i < player.path?.length; i++) {
        context.beginPath();
        context.fillStyle = player.color;
        context.arc(player.path[i][0], player.path[i][1], player.radius / 2, 0, Math.PI * 2);
        context.fill();
        context.lineWidth = 3;
        context.strokeStyle = 'rgb(0,255,0)';
        context.stroke();
    }
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

// const socket = new WebSocket('ws://localhost:8090/ws');
const socket = new WebSocket('ws://5.135.186.53:8090/ws');

function init() {
    draw()
    const sid =  generateSessionId()
    player = { ...player, sid }
    socket.send(JSON.stringify({
        _type: 'InitMessage',
        data: {
            playerName: player.name,
            sid
        }
    }));
}

function generateSessionId() {
    return Date.now().toString(36) + Math.random().toString(36).substring(2)
}

socket.addEventListener('message', (event) => {
    const message = JSON.parse(event.data);
    switch (message && message._type) {
        case 'InitMessageResponse':
            orbs = message.data.orbs
            if (player.sid === message.data.playerData.sid) {
                player = {...player, ...message.data.playerData}
                setInterval(() => {
                    if (socket.readyState === WebSocket.OPEN && player.uid && player.xVector) socket.send(JSON.stringify({
                        _type: 'TickMessage',
                        data: {
                            uid: player.uid,
                            xVector: player.xVector || 0,
                            yVector: player.yVector || 0
                        }
                    }));
                }, 33);
            }
            break;
        case 'PlayerListMessageResponse':
            players = message.data;
            updateLeaderBoard();
            break;
        case 'TickMessageResponse':
            if(player.sid === message.data.playerData.sid) {
                player = {...player, ...message.data.playerData};
                orbs = message.data.orbs;
                updatePlayerScore();
            }
            break;
    }
});

socket.onclose = function (e) {
    if (player.sid) {
        $('#game-start').modal('hide')
        $('#game-over').modal('show')
    }
}
