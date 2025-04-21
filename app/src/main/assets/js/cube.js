    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(70, window.innerWidth / window.innerHeight, 0.1, 1000);
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.setPixelRatio(window.devicePixelRatio);
    document.body.appendChild(renderer.domElement);

    const colors = [0xffff00, 0xffffff, 0x0000ff, 0x00ff00, 0xff0000, 0xffa500, 0x33ffff];
    const materials = colors.map(c => new THREE.MeshPhongMaterial({ color: c }));
    const cubes = [];

    for (let x = -1; x <= 1; x++) {
        for (let y = -1; y <= 1; y++) {
            for (let z = -1; z <= 1; z++) {
                const geom = new THREE.BoxGeometry(0.95, 0.95, 0.95);
                const cube = new THREE.Mesh(geom, [
                    x === 1 ? materials[0] : materials[6],
                    x === -1 ? materials[1] : materials[6],
                    y === 1 ? materials[2] : materials[6],
                    y === -1 ? materials[3] : materials[6],
                    z === 1 ? materials[4] : materials[6],
                    z === -1 ? materials[5] : materials[6]
                ]);
                cube.position.set(x, y, z);
                scene.add(cube);
                cubes.push(cube);
            }
        }
    }

    camera.position.set(3,3,3);
    camera.lookAt(0, 0, 0);
    scene.add(new THREE.DirectionalLight(0xffffff, 1).position.set(5, 10, 10));
    scene.add(new THREE.AmbientLight(0xffffff, 0.4));

    const rotationGroup = new THREE.Group();
    scene.add(rotationGroup);

    let touchStart = new THREE.Vector2();
    let pickedCube = null;
    let rotateAxis = null;
    let rotateLayer = [];
    let rotating = false;
    let targetAngle = 0;
    let rotateAngle = 0;
    let clickedFaceNormal = null;
    let scrambleQueue = [];//打乱步骤队列
    let isScrambling = false;
    let moveStack = [];//用户操作栈
    let timerFlat = false;//计时器标志
    let isUndo = false;
    let isSyn = false;//同步联机状态
    const axisMap = {
        'R': ['x', 1, 1],
        'L': ['x', -1, 1],
        'U': ['y', 1, 1],
        'D': ['y', -1, 1],
        'F': ['z', 1, 1],
        'B': ['z', -1, 1],

        // 逆时针（带'后缀）
        "R'": ['x', 1, -1],
        "L'": ['x', -1, -1],
        "U'": ['y', 1, -1],
        "D'": ['y', -1, -1],
        "F'": ['z', 1, -1],
        "B'": ['z', -1, -1],

        // 180度（带2后缀）
        'R2': ['x', 1, 2],
        'L2': ['x', -1, 2],
        'U2': ['y', 1, 2],
        'D2': ['y', -1, 2],
        'F2': ['z', 1, 2],
        'B2': ['z', -1, 2]
    };

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
        }
    });

    function determineAxis(faceNormal, dx, dy) {
        const absX = Math.abs(faceNormal.x);
        const absY = Math.abs(faceNormal.y);
        const absZ = Math.abs(faceNormal.z);

        // 确定主法线方向
        const mainAxis = absX > absY ?
            (absX > absZ ? 'x' : 'z') :
            (absY > absZ ? 'y' : 'z');

        // 根据主法线方向选择旋转轴
        if (mainAxis === 'x') { // 点击的是左右面
            return Math.abs(dx) > Math.abs(dy) ? 'y' : 'z';
        } else if (mainAxis === 'y') { // 点击的是顶底面
            return Math.abs(dx) > Math.abs(dy) ? 'z' : 'x';
        } else { // 点击的是前后面
            return Math.abs(dx) > Math.abs(dy) ? 'y' : 'x';
        }
    }

    renderer.domElement.addEventListener('touchmove', e => {
        e.preventDefault();
        if (!pickedCube || rotating || !clickedFaceNormal) return;

        const dx = e.touches[0].clientX - touchStart.x;
        const dy = e.touches[0].clientY - touchStart.y;

        // 确定旋转轴
        rotateAxis = determineAxis(clickedFaceNormal, dx, dy);

        let angleSign = 1;
        const mainAxis = clickedFaceNormal.x !== 0 ? 'x' :
            clickedFaceNormal.y !== 0 ? 'y' : 'z';

        // 根据滑动方向调整角度符号
        switch (mainAxis) {
            case 'x':
                if (rotateAxis === 'y') angleSign = dx > 0 ? 1 : -1;
                if (rotateAxis === 'z') angleSign = dy > 0 ? -1 : 1;
                break;
            case 'y':
                if (rotateAxis === 'x') {
                    angleSign = dy > 0 ? -1 : 1;
                }
                if (rotateAxis === 'z') {
                    angleSign = dx > 0 ? -1 : 1;
                }
                break;
            case 'z':
                if (rotateAxis === 'x') angleSign = dy > 0 ? 1 : -1;
                if (rotateAxis === 'y') angleSign = dx > 0 ? -1 : 1;
                break;
        }

        targetAngle = angleSign*Math.PI / 2;

        // 确定要旋转的层
        const layerValue = Math.round(pickedCube.position[rotateAxis]);
        rotateLayer = getLayer(rotateAxis, layerValue);

        // 将层添加到旋转组
        rotationGroup.clear();
        rotationGroup.rotation.set(0, 0, 0);
        rotateLayer.forEach(cube => {
            rotationGroup.attach(cube);
        });

        rotating = true;
        rotateAngle = 0;
        pickedCube = null;
        clickedFaceNormal = null;
    });

    function animate() {
        requestAnimationFrame(animate);

        if(!timerFlat&&!isScrambling&&rotating){
            timerFlat = true;
            if (typeof Android !== 'undefined') {
                Android.onRotateStart();
            }
        }

        if (rotating || isScrambling) {
            var target=targetAngle;
            const delta = (targetAngle - rotateAngle) * 0.4;
            rotateAngle += delta;

            const axisVec = new THREE.Vector3(
                rotateAxis === 'x' ? 1 : 0,
                rotateAxis === 'y' ? 1 : 0,
                rotateAxis === 'z' ? 1 : 0
            );

            rotationGroup.rotateOnAxis(axisVec, delta);

            if (Math.abs(rotateAngle - targetAngle) < 0.01) {
                const finalDelta = targetAngle - rotateAngle;
                rotationGroup.rotateOnAxis(axisVec, finalDelta);

                rotateLayer.forEach(cube => {
                    scene.attach(cube);
                });
                rotationGroup.clear();

                rotating = false;
                rotateAngle = 0;
                targetAngle = 0;

                //用户操作入栈
                if (!isScrambling&&!isUndo) {
                    moveStack.push({
                        axis: rotateAxis,
                        value: Math.round(rotateLayer[0].position[rotateAxis]),
                        angle: target
                    });
                    //处于多人模式中
                        if (typeof Android !== 'undefined'&& window.isMultipleMode) {
                            //操作者
                            if(!isSyn)
                            Android.sendMove(
                                rotateAxis,
                                Math.round(rotateLayer[0].position[rotateAxis]),
                                target
                            );
                            //同步者
                            else{
                                console.log("正在同步远程操作");
                                isSyn=false;
                            }
                        }
                }

                else if (isScrambling && scrambleQueue.length > 0) {
                    executeScrambleStep();
                } else if (isScrambling && scrambleQueue.length === 0) {
                    isScrambling = false;
                }

                //已经还原
                if(!isScrambling&&isCubeSolved()){
                    if (typeof Android !== 'undefined') {
                        Android.onCubeSolved();
                    }
                }
            }
        }
        renderer.render(scene, camera);
    }


function scrambleCube(str) {
    moveStack.length = 0;
    if (rotating || isScrambling) return;

    for(let i = 0;i<str.length;++i){
        if(i!==str.length-1&&(str[i+1]==='\'' || str[i+1]==='2')){
            scrambleQueue.push(str[i]+str[i+1]);
            i++;
        }
        else
            scrambleQueue.push(str[i]);
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

    const [axis, value, angleType] = axisMap[move];

    const angle = angleType === 2 ? Math.PI : angleType * Math.PI/2;

    rotateAxis = axis;
    targetAngle = angle;
    rotateAngle = 0;
    rotating = true;

    rotateLayer = getLayer(axis, value);
    rotationGroup.clear();
    rotationGroup.rotation.set(0, 0, 0);
    rotateLayer.forEach(cube => {
        rotationGroup.attach(cube);
    });
    }

function undoMove() {
        if(isCubeSolved()){
            moveStack.length=0;
        }
        else if (moveStack.length === 0) {
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
            rotateLayer.forEach(cube => {
                rotationGroup.attach(cube);
            });
        //防止undo加入操作栈
        isScrambling=true;
        return true;
}

function isCubeSolved() {
    for (const cube of cubes) {
        const pos = cube.position;
        const rot = cube.rotation;
        if (
            Math.abs(pos.x) > 1.1 ||
            Math.abs(pos.y) > 1.1 ||
            Math.abs(pos.z) > 1.1 ||
            Math.abs(rot.x) > 0.01 ||
            Math.abs(rot.y) > 0.01 ||
            Math.abs(rot.z) > 0.01
        ) {
            return false;
        }
    }
    return true;
}

function applyRemoteMove(axis, value, angle) {
    isSyn = true;
    rotateAxis = axis;
    targetAngle = angle===1?Math.PI/2:-Math.PI/2;
    rotateAngle = 0;
    rotating = true;

    rotateLayer = getLayer(axis, value);
    rotationGroup.clear();
    rotationGroup.rotation.set(0, 0, 0);
    rotateLayer.forEach(cube => {
        rotationGroup.attach(cube);
    });
}
    animate();

    window.addEventListener('resize', () => {
        camera.aspect = window.innerWidth / window.innerHeight;
        camera.updateProjectionMatrix();
        renderer.setSize(window.innerWidth, window.innerHeight);
    });