const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(70, window.innerWidth / window.innerHeight, 0.1, 1000);
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(window.devicePixelRatio);
document.body.appendChild(renderer.domElement);

const colors = [0xffff00, 0xffffff, 0x0000ff, 0x00ff00, 0xff0000, 0xffa500, 0x33ffff];
const materials = colors.map(c => new THREE.MeshPhongMaterial({ color: c }));
const cubes = [];

let level = new URLSearchParams(window.location.search).get("level");
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

scene.add(new THREE.DirectionalLight(0xffffff, 1).position.set(5, 10, 10));
scene.add(new THREE.AmbientLight(0xffffff, 0.4));

let cameraDistance = 5;
let cameraAlpha = Math.PI/4; // 水平旋转角
let cameraBeta = Math.PI/4;  // 垂直俯仰角
let isDragging = false;
let lastTouch = new THREE.Vector2();

updateCameraPosition();

renderer.domElement.addEventListener('touchstart', e => {
    isDragging = true;
    lastTouch.x = e.touches[0].clientX;
    lastTouch.y = e.touches[0].clientY;
});

renderer.domElement.addEventListener('touchmove', e => {
    if (!isDragging) return;

    const deltaX = e.touches[0].clientX - lastTouch.x;
    const deltaY = e.touches[0].clientY - lastTouch.y;

    cameraAlpha += deltaX * 0.01;

    cameraBeta -= deltaY * 0.01;

    updateCameraPosition();

    lastTouch.x = e.touches[0].clientX;
    lastTouch.y = e.touches[0].clientY;
});

renderer.domElement.addEventListener('touchend', () => {
    isDragging = false;
});

function updateCameraPosition() {
    const baseDistance = 6; // 可调整基础距离
    const scaleFactor = level / 3; // 相对3阶魔方的缩放因子
    const distance = baseDistance * scaleFactor;

    const x = distance * Math.sin(cameraBeta) * Math.cos(cameraAlpha);
    const y = distance * Math.cos(cameraBeta);
    const z = distance * Math.sin(cameraBeta) * Math.sin(cameraAlpha);

    camera.position.set(x, y, z);
    camera.lookAt(0, 0, 0);
}

// 渲染循环
function animate() {
    requestAnimationFrame(animate);
    renderer.render(scene, camera);
}

// 窗口大小变化处理
window.addEventListener('resize', () => {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
});


animate();