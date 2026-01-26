import { Konva } from "./_CoreInternals.js";
import { Canvas, DOMMatrix, Image, Path2D } from 'skia-canvas';
global.DOMMatrix = DOMMatrix;
global.Path2D = Path2D;
Path2D.prototype.toString = () => '[object Path2D]';
Konva.Util['createCanvasElement'] = () => {
    const node = new Canvas(300, 300);
    if (!node['style']) {
        node['style'] = {};
    }
    node.toString = () => '[object HTMLCanvasElement]';
    const ctx = node.getContext('2d');
    Object.defineProperty(ctx, 'canvas', {
        get: () => node,
    });
    return node;
};
Konva.Util.createImageElement = () => {
    const node = new Image();
    node.toString = () => '[object HTMLImageElement]';
    return node;
};
Konva._renderBackend = 'skia-canvas';
export default Konva;
