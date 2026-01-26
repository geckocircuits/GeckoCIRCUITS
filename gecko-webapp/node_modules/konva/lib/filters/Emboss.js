import { Factory } from "../Factory.js";
import { Node } from "../Node.js";
import { getNumberValidator } from "../Validators.js";
export const Emboss = function (imageData) {
    var _a, _b, _c, _d, _e, _f, _g, _h, _j;
    const data = imageData.data;
    const w = imageData.width;
    const h = imageData.height;
    const strength01 = Math.min(1, Math.max(0, (_b = (_a = this.embossStrength) === null || _a === void 0 ? void 0 : _a.call(this)) !== null && _b !== void 0 ? _b : 0.5));
    const whiteLevel01 = Math.min(1, Math.max(0, (_d = (_c = this.embossWhiteLevel) === null || _c === void 0 ? void 0 : _c.call(this)) !== null && _d !== void 0 ? _d : 0.5));
    const directionMap = {
        'top-left': 315,
        top: 270,
        'top-right': 225,
        right: 180,
        'bottom-right': 135,
        bottom: 90,
        'bottom-left': 45,
        left: 0,
    };
    const directionDeg = (_g = directionMap[(_f = (_e = this.embossDirection) === null || _e === void 0 ? void 0 : _e.call(this)) !== null && _f !== void 0 ? _f : 'top-left']) !== null && _g !== void 0 ? _g : 315;
    const blend = !!((_j = (_h = this.embossBlend) === null || _h === void 0 ? void 0 : _h.call(this)) !== null && _j !== void 0 ? _j : false);
    const strength = strength01 * 10;
    const bias = whiteLevel01 * 255;
    const dirRad = (directionDeg * Math.PI) / 180;
    const cx = Math.cos(dirRad);
    const cy = Math.sin(dirRad);
    const SCALE = (128 / 1020) * strength;
    const src = new Uint8ClampedArray(data);
    const lum = new Float32Array(w * h);
    for (let p = 0, i = 0; i < data.length; i += 4, p++) {
        lum[p] = 0.2126 * src[i] + 0.7152 * src[i + 1] + 0.0722 * src[i + 2];
    }
    const Gx = [-1, 0, 1, -2, 0, 2, -1, 0, 1];
    const Gy = [-1, -2, -1, 0, 0, 0, 1, 2, 1];
    const OFF = [-w - 1, -w, -w + 1, -1, 0, 1, w - 1, w, w + 1];
    const clamp8 = (v) => (v < 0 ? 0 : v > 255 ? 255 : v);
    for (let y = 1; y < h - 1; y++) {
        for (let x = 1; x < w - 1; x++) {
            const p = y * w + x;
            let sx = 0, sy = 0;
            sx += lum[p + OFF[0]] * Gx[0];
            sy += lum[p + OFF[0]] * Gy[0];
            sx += lum[p + OFF[1]] * Gx[1];
            sy += lum[p + OFF[1]] * Gy[1];
            sx += lum[p + OFF[2]] * Gx[2];
            sy += lum[p + OFF[2]] * Gy[2];
            sx += lum[p + OFF[3]] * Gx[3];
            sy += lum[p + OFF[3]] * Gy[3];
            sx += lum[p + OFF[5]] * Gx[5];
            sy += lum[p + OFF[5]] * Gy[5];
            sx += lum[p + OFF[6]] * Gx[6];
            sy += lum[p + OFF[6]] * Gy[6];
            sx += lum[p + OFF[7]] * Gx[7];
            sy += lum[p + OFF[7]] * Gy[7];
            sx += lum[p + OFF[8]] * Gx[8];
            sy += lum[p + OFF[8]] * Gy[8];
            const r = cx * sx + cy * sy;
            const outGray = clamp8(bias + r * SCALE);
            const o = p * 4;
            if (blend) {
                const delta = outGray - bias;
                data[o] = clamp8(src[o] + delta);
                data[o + 1] = clamp8(src[o + 1] + delta);
                data[o + 2] = clamp8(src[o + 2] + delta);
                data[o + 3] = src[o + 3];
            }
            else {
                data[o] = data[o + 1] = data[o + 2] = outGray;
                data[o + 3] = src[o + 3];
            }
        }
    }
    for (let x = 0; x < w; x++) {
        let oTop = x * 4, oBot = ((h - 1) * w + x) * 4;
        data[oTop] = src[oTop];
        data[oTop + 1] = src[oTop + 1];
        data[oTop + 2] = src[oTop + 2];
        data[oTop + 3] = src[oTop + 3];
        data[oBot] = src[oBot];
        data[oBot + 1] = src[oBot + 1];
        data[oBot + 2] = src[oBot + 2];
        data[oBot + 3] = src[oBot + 3];
    }
    for (let y = 1; y < h - 1; y++) {
        let oL = y * w * 4, oR = (y * w + (w - 1)) * 4;
        data[oL] = src[oL];
        data[oL + 1] = src[oL + 1];
        data[oL + 2] = src[oL + 2];
        data[oL + 3] = src[oL + 3];
        data[oR] = src[oR];
        data[oR + 1] = src[oR + 1];
        data[oR + 2] = src[oR + 2];
        data[oR + 3] = src[oR + 3];
    }
    return imageData;
};
Factory.addGetterSetter(Node, 'embossStrength', 0.5, getNumberValidator(), Factory.afterSetFilter);
Factory.addGetterSetter(Node, 'embossWhiteLevel', 0.5, getNumberValidator(), Factory.afterSetFilter);
Factory.addGetterSetter(Node, 'embossDirection', 'top-left', undefined, Factory.afterSetFilter);
Factory.addGetterSetter(Node, 'embossBlend', false, undefined, Factory.afterSetFilter);
