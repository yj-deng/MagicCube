const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(70, window.innerWidth / window.innerHeight, 0.1, 1000);
const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
renderer.setSize(window.innerWidth, window.innerHeight);
renderer.setPixelRatio(window.devicePixelRatio);
document.body.appendChild(renderer.domElement);

// 初始化魔方但不添加交互
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

// 视角控制参数
let cameraDistance = 5;
let cameraAlpha = Math.PI/4; // 水平旋转角
let cameraBeta = Math.PI/4;  // 垂直俯仰角
let isDragging = false;
let lastTouch = { x: 0, y: 0 };

// 初始化相机位置
updateCameraPosition();

// 触摸事件处理
renderer.domElement.addEventListener('touchstart', e => {
    isDragging = true;
    lastTouch.x = e.touches[0].clientX;
    lastTouch.y = e.touches[0].clientY;
});

renderer.domElement.addEventListener('touchmove', e => {
    if (!isDragging) return;

    const deltaX = e.touches[0].clientX - lastTouch.x;
    const deltaY = e.touches[0].clientY - lastTouch.y;

    // 水平滑动控制水平旋转
    cameraAlpha += deltaX * 0.01;

    // 垂直滑动控制垂直角度（限制范围）
    cameraBeta = cameraBeta - deltaY * 0.01;

    updateCameraPosition();

    lastTouch.x = e.touches[0].clientX;
    lastTouch.y = e.touches[0].clientY;
});

renderer.domElement.addEventListener('touchend', () => {
    isDragging = false;
});

// 更新相机位置函数
function updateCameraPosition() {
    camera.position.x = cameraDistance * Math.cos(cameraAlpha) * Math.sin(cameraBeta);
    camera.position.y = cameraDistance * Math.cos(cameraBeta);
    camera.position.z = cameraDistance * Math.sin(cameraAlpha) * Math.sin(cameraBeta);
    camera.lookAt(scene.position);
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

// 添加光源
scene.add(new THREE.DirectionalLight(0xffffff, 1).position.set(5, 10, 10));
scene.add(new THREE.AmbientLight(0xffffff, 0.4));

animate();