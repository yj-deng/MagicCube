const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(70, window.innerWidth / window.innerHeight, 0.1, 1000);
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(window.devicePixelRatio);
document.body.appendChild(renderer.domElement);

const colors = [0xffff00, 0xffffff, 0x0000ff, 0x00ff00, 0xff0000, 0xffa500, 0x33ffff];
const materials = colors.map(c => new THREE.MeshPhongMaterial({ color: c }));
const cubes = [];

let level = new URLSearchParams(window.location.search).get("level")
let cubeValue = Math.floor(level/2);

InitCubes();

function InitCubes(){
for (let x = -cubeValue; x <= cubeValue; x++) {
    for (let y = -cubeValue; y <= cubeValue; y++) {
        for (let z = -cubeValue; z <= cubeValue; z++) {
            const geom = new THREE.BoxGeometry(0.95,0.95,0.95);
            const cube = new THREE.Mesh(geom, [
                x === cubeValue ? materials[0] : materials[6],
                x === -cubeValue ? materials[1] : materials[6],
                y === cubeValue ? materials[2] : materials[6],
                y === -cubeValue ? materials[3] : materials[6],
                z === cubeValue ? materials[4] : materials[6],
                z === -cubeValue ? materials[5] : materials[6]
            ]);
            cube.position.set(x,y,z);
            scene.add(cube);
            cubes.push(cube);
        }
    }
}
}

const rotationGroup = new THREE.Group();
scene.add(rotationGroup);

scene.add(new THREE.DirectionalLight(0xffffff, 1).position.set(5, 10, 10));
scene.add(new THREE.AmbientLight(0xffffff, 0.4));

//控制视角移动
let cameraDistance = 5;
let cameraAlpha = Math.PI / 4;
let cameraBeta = Math.PI / 4;
let isDragging = false;
let lastTouch = new THREE.Vector2();

function updateCameraPosition() {
    const baseDistance = 5;
    const scaleFactor = level / 3;
    const distance = baseDistance * scaleFactor;

    const x = distance * Math.sin(cameraBeta) * Math.cos(cameraAlpha);
    const y = distance * Math.cos(cameraBeta);
    const z = distance * Math.sin(cameraBeta) * Math.sin(cameraAlpha);

    camera.position.set(x, y, z);
    camera.lookAt(0, 0, 0);
}

updateCameraPosition();

// ===== 魔方控制变量 =====
let touchStart = new THREE.Vector2();
let pickedCube = null;
let rotateAxis = null;
let rotateLayer = [];
let rotating = false;
let targetAngle = 0;
let rotateAngle = 0;
let clickedFaceNormal = null;
let scrambleQueue = [];
let isScrambling = false;
let moveStack = [];
let timerFlat = false;
let isUndo = false;
let isSyn = false;
let isRestore = false;
let restoreMove = [];
let enableVibrate = true;

function getLayer(axis, value) {
    return cubes.filter(c => Math.round(c.position[axis]) === value);
}

renderer.domElement.addEventListener('touchstart', e => {
    if (rotating) return;
    const touch = e.touches[0];
    touchStart.set(touch.clientX, touch.clientY);

    const mouse = new THREE.Vector2(
        (touch.clientX / window.innerWidth) * 2 - 1,
        -(touch.clientY / window.innerHeight) * 2 + 1
    );

    const raycaster = new THREE.Raycaster();
    raycaster.setFromCamera(mouse, camera);
    const intersects = raycaster.intersectObjects(cubes);

    if (intersects.length > 0) {
        pickedCube = intersects[0].object;
        clickedFaceNormal = intersects[0].face.normal
            .clone()
            .applyMatrix4(pickedCube.matrixWorld)
            .normalize();
    } else {
        isDragging = true;
        lastTouch.set(touch.clientX, touch.clientY);
    }
});

renderer.domElement.addEventListener('touchmove', e => {
    e.preventDefault();
    if (isDragging) {
        const dx = e.touches[0].clientX - lastTouch.x;
        const dy = e.touches[0].clientY - lastTouch.y;

        cameraAlpha += dx * 0.005;
        cameraBeta -= dy * 0.005;

        cameraBeta = Math.max(0.1, Math.min(Math.PI - 0.1, cameraBeta));
        updateCameraPosition();

        lastTouch.set(e.touches[0].clientX, e.touches[0].clientY);
        return;
    }

    if (!pickedCube || rotating || !clickedFaceNormal) return;

    const dx = e.touches[0].clientX - touchStart.x;
    const dy = e.touches[0].clientY - touchStart.y;

    rotateAxis = determineAxisWithCamera(touchStart, new THREE.Vector2(e.touches[0].clientX, e.touches[0].clientY), clickedFaceNormal);

    let angleSign = 1;
    const mainAxis = clickedFaceNormal.x !== 0 ? 'x' :
        clickedFaceNormal.y !== 0 ? 'y' : 'z';

    switch (mainAxis) {
        case 'x':
            if (rotateAxis === 'y') angleSign = dx > 0 ? 1 : -1;
            if (rotateAxis === 'z') angleSign = dy > 0 ? -1 : 1;
            break;
        case 'y':
            if (rotateAxis === 'x') angleSign = dy > 0 ? -1 : 1;
            if (rotateAxis === 'z') angleSign = dx > 0 ? -1 : 1;
            break;
        case 'z':
            if (rotateAxis === 'x') angleSign = dy > 0 ? 1 : -1;
            if (rotateAxis === 'y') angleSign = dx > 0 ? -1 : 1;
            break;
    }

    targetAngle = angleSign * Math.PI / 2;
    const layerValue = Math.round(pickedCube.position[rotateAxis]);
    rotateLayer = getLayer(rotateAxis, layerValue);

    rotationGroup.clear();
    rotationGroup.rotation.set(0, 0, 0);
    rotateLayer.forEach(cube => rotationGroup.attach(cube));

    rotating = true;
    rotateAngle = 0;
    pickedCube = null;
    clickedFaceNormal = null;
});

renderer.domElement.addEventListener('touchend', () => {
    isDragging = false;
});

function determineAxisWithCamera(touchStart, touchEnd, faceNormal) {
    const startNDC = new THREE.Vector3(
        (touchStart.x / window.innerWidth) * 2 - 1,
        -(touchStart.y / window.innerHeight) * 2 + 1,
        0.5
    );
    const endNDC = new THREE.Vector3(
        (touchEnd.x / window.innerWidth) * 2 - 1,
        -(touchEnd.y / window.innerHeight) * 2 + 1,
        0.5
    );

    const startWorld = startNDC.clone().unproject(camera);
    const endWorld = endNDC.clone().unproject(camera);

    const moveDirection = endWorld.sub(startWorld).normalize();

    const axisVector = new THREE.Vector3().crossVectors(faceNormal, moveDirection).normalize();

    const absX = Math.abs(axisVector.x);
    const absY = Math.abs(axisVector.y);
    const absZ = Math.abs(axisVector.z);

    if (absX > absY && absX > absZ) return 'x';
    if (absY > absZ) return 'y';
    return 'z';
}


function animate() {
    requestAnimationFrame(animate);

    if (!timerFlat && !isScrambling && rotating) {
        timerFlat = true;
        if (typeof Android !== 'undefined') {
            Android.onRotateStart();
        }
    }

    if (rotating || isScrambling || isRestore || isUndo) {
        var target=targetAngle;
        const delta = (targetAngle - rotateAngle) * 0.2;
        rotateAngle += delta;

        const axisVec = new THREE.Vector3(
            rotateAxis === 'x' ? 1 : 0,
            rotateAxis === 'y' ? 1 : 0,
            rotateAxis === 'z' ? 1 : 0
        );

        rotationGroup.rotateOnAxis(axisVec, delta);

        //补齐90度
        if (Math.abs(rotateAngle - targetAngle) < 0.01) {
            const finalDelta = targetAngle - rotateAngle;
            rotationGroup.rotateOnAxis(axisVec, finalDelta);

            rotateLayer.forEach(cube => scene.attach(cube));
            rotationGroup.clear();

            rotating = false;
            rotateAngle = 0;
            targetAngle = 0;

//            console.log(rotateAxis);

            if (!isRestore) {
                restoreMove.push({
                    axis: rotateAxis,
                    value: Math.round(rotateLayer[0].position[rotateAxis]),
                    angle: target
                });
            }
            //用户操作入栈
            if (!isScrambling && !isUndo && !isRestore) {
                if (navigator.vibrate && enableVibrate) {
                    navigator.vibrate(100);
                }

                moveStack.push({
                    axis: rotateAxis,
                    value: Math.round(rotateLayer[0].position[rotateAxis]),
                    angle: target
                });
                //多人模式
                if (typeof Android !== 'undefined' && window.isMultipleMode) {
                    if (!isSyn) {
                        //操作者
                        Android.sendMove(
                            rotateAxis,
                            Math.round(rotateLayer[0].position[rotateAxis]),
                            target
                        );
                    } else {
                        //同步者
                        isSyn = false;
                    }
                }
            } else if (isScrambling && scrambleQueue.length > 0) {
                executeScrambleStep();
            } else if (isScrambling && scrambleQueue.length === 0) {
                isScrambling = false;
            } else if (isRestore && restoreMove.length > 0) {
                executeRestoreStep();
            } else if (isRestore && restoreMove.length === 0) {
                isRestore = false;
            }
            //用户操作还原
            if (!isScrambling && isCubeSolved() && !isRestore && !isUndo) {
                if (typeof Android !== 'undefined') {
                    timerFlat = false;
                    Android.onCubeSolved();
                }
            }
        }
    }

    renderer.render(scene, camera);
}

function scrambleCube(str) {
    moveStack.length = 0;
    if (rotating || isScrambling || isRestore) return;

    const regex = /([xyz])(-?\d)(-?\d)/g;
    let match;
    while ((match = regex.exec(str)) !== null) {
        scrambleQueue.push({
            axis: match[1],
            value: parseInt(match[2], 10),
            angleType: parseInt(match[3], 10)
        });
    }

    isScrambling = true;
    executeScrambleStep();
}

function executeScrambleStep() {
    if (scrambleQueue.length === 0) {
        isScrambling = false;
        return;
    }

    const move = scrambleQueue.shift();
    const angle = move.angleType === 2 ? Math.PI : move.angleType * Math.PI / 2;
    rotateAxis = move.axis;
    targetAngle = angle;
    rotateAngle = 0;
    rotating = true;
    rotateLayer = getLayer(rotateAxis, move.value);

    rotationGroup.clear();
    rotationGroup.rotation.set(0, 0, 0);
    rotateLayer.forEach(cube => rotationGroup.attach(cube));
}

function applyRemoteMove(axis, value, angle) {
    isSyn = true;
    rotateAxis = axis;
    targetAngle = angle === 1 ? Math.PI / 2 : -Math.PI / 2;
    rotateAngle = 0;
    rotating = true;
    rotateLayer = getLayer(axis, value);
    rotationGroup.clear();
    rotationGroup.rotation.set(0, 0, 0);
    rotateLayer.forEach(cube => rotationGroup.attach(cube));
}

function undoMove() {
    if (isCubeSolved()) {
        moveStack.length = 0;
    } else if (moveStack.length === 0) {
        return false;
    }
    const lastMove = moveStack.pop();

    rotateAxis = lastMove.axis;
    targetAngle = -lastMove.angle;
    rotateAngle = 0;
    rotating = true;
    rotateLayer = getLayer(rotateAxis, lastMove.value);
    rotationGroup.clear();
    rotationGroup.rotation.set(0, 0, 0);
    rotateLayer.forEach(cube => rotationGroup.attach(cube));
    isUndo = true;
    return true;
}

function isCubeSolved() {
    for (const cube of cubes) {
        const pos = cube.position;
        const rot = cube.rotation;
        if (
            Math.abs(pos.x) > cubeValue+0.1 ||
            Math.abs(pos.y) > cubeValue+0.1 ||
            Math.abs(pos.z) > cubeValue+0.1 ||
            Math.abs(rot.x) > 0.01 ||
            Math.abs(rot.y) > 0.01 ||
            Math.abs(rot.z) > 0.01
        ) {
            return false;
        }
    }
    return true;
}

function restore() {
    moveStack.length = 0;
    if (rotating || isScrambling || isRestore) return;
    isRestore = true;
    executeRestoreStep();
}

function executeRestoreStep() {
    if (restoreMove.length === 0) {
        isRestore = false;
        return;
    }
    const move = restoreMove.pop();
    rotateAxis = move.axis;
    targetAngle = -move.angle;
    rotateAngle = 0;
    rotating = true;
    rotateLayer = getLayer(rotateAxis, move.value);
    rotationGroup.clear();
    rotationGroup.rotation.set(0, 0, 0);
    rotateLayer.forEach(cube => rotationGroup.attach(cube));
}

animate();

window.addEventListener('resize', () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
});
