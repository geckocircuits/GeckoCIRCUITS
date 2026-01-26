import { Util } from "../Util.js";
import { Factory } from "../Factory.js";
import { Shape } from "../Shape.js";
import { Path } from "./Path.js";
import { Text, stringToArray } from "./Text.js";
import { getNumberValidator } from "../Validators.js";
import { _registerNode } from "../Global.js";
const EMPTY_STRING = '', NORMAL = 'normal';
function _fillFunc(context) {
    context.fillText(this.partialText, 0, 0);
}
function _strokeFunc(context) {
    context.strokeText(this.partialText, 0, 0);
}
export class TextPath extends Shape {
    constructor(config) {
        super(config);
        this.dummyCanvas = Util.createCanvasElement();
        this.dataArray = [];
        this._readDataAttribute();
        this.on('dataChange.konva', function () {
            this._readDataAttribute();
            this._setTextData();
        });
        this.on('textChange.konva alignChange.konva letterSpacingChange.konva kerningFuncChange.konva fontSizeChange.konva fontFamilyChange.konva', this._setTextData);
        this._setTextData();
    }
    _getTextPathLength() {
        return Path.getPathLength(this.dataArray);
    }
    _getPointAtLength(length) {
        if (!this.attrs.data) {
            return null;
        }
        const totalLength = this.pathLength;
        if (length > totalLength) {
            return null;
        }
        return Path.getPointAtLengthOfDataArray(length, this.dataArray);
    }
    _readDataAttribute() {
        this.dataArray = Path.parsePathData(this.attrs.data);
        this.pathLength = this._getTextPathLength();
    }
    _sceneFunc(context) {
        context.setAttr('font', this._getContextFont());
        context.setAttr('textBaseline', this.textBaseline());
        context.setAttr('textAlign', 'left');
        context.save();
        const textDecoration = this.textDecoration();
        const fill = this.fill();
        const fontSize = this.fontSize();
        const glyphInfo = this.glyphInfo;
        const hasUnderline = textDecoration.indexOf('underline') !== -1;
        const hasLineThrough = textDecoration.indexOf('line-through') !== -1;
        if (hasUnderline) {
            context.beginPath();
        }
        for (let i = 0; i < glyphInfo.length; i++) {
            context.save();
            const p0 = glyphInfo[i].p0;
            context.translate(p0.x, p0.y);
            context.rotate(glyphInfo[i].rotation);
            this.partialText = glyphInfo[i].text;
            context.fillStrokeShape(this);
            if (hasUnderline) {
                if (i === 0) {
                    context.moveTo(0, fontSize / 2 + 1);
                }
                context.lineTo(glyphInfo[i].width, fontSize / 2 + 1);
            }
            context.restore();
        }
        if (hasUnderline) {
            context.strokeStyle = fill;
            context.lineWidth = fontSize / 20;
            context.stroke();
        }
        if (hasLineThrough) {
            context.beginPath();
            for (let i = 0; i < glyphInfo.length; i++) {
                context.save();
                const p0 = glyphInfo[i].p0;
                context.translate(p0.x, p0.y);
                context.rotate(glyphInfo[i].rotation);
                if (i === 0) {
                    context.moveTo(0, 0);
                }
                context.lineTo(glyphInfo[i].width, 0);
                context.restore();
            }
            context.strokeStyle = fill;
            context.lineWidth = fontSize / 20;
            context.stroke();
        }
        context.restore();
    }
    _hitFunc(context) {
        context.beginPath();
        const glyphInfo = this.glyphInfo;
        if (glyphInfo.length >= 1) {
            const p0 = glyphInfo[0].p0;
            context.moveTo(p0.x, p0.y);
        }
        for (let i = 0; i < glyphInfo.length; i++) {
            const p1 = glyphInfo[i].p1;
            context.lineTo(p1.x, p1.y);
        }
        context.setAttr('lineWidth', this.fontSize());
        context.setAttr('strokeStyle', this.colorKey);
        context.stroke();
    }
    getTextWidth() {
        return this.textWidth;
    }
    getTextHeight() {
        Util.warn('text.getTextHeight() method is deprecated. Use text.height() - for full height and text.fontSize() - for one line height.');
        return this.textHeight;
    }
    setText(text) {
        return Text.prototype.setText.call(this, text);
    }
    _getContextFont() {
        return Text.prototype._getContextFont.call(this);
    }
    _getTextSize(text) {
        const dummyCanvas = this.dummyCanvas;
        const _context = dummyCanvas.getContext('2d');
        _context.save();
        _context.font = this._getContextFont();
        const metrics = _context.measureText(text);
        _context.restore();
        return {
            width: metrics.width,
            height: parseInt(`${this.fontSize()}`, 10),
        };
    }
    _setTextData() {
        const charArr = stringToArray(this.text());
        const chars = [];
        let width = 0;
        for (let i = 0; i < charArr.length; i++) {
            chars.push({
                char: charArr[i],
                width: this._getTextSize(charArr[i]).width,
            });
            width += chars[i].width;
        }
        const { height } = this._getTextSize(this.attrs.text);
        this.textWidth = width;
        this.textHeight = height;
        this.glyphInfo = [];
        if (!this.attrs.data) {
            return null;
        }
        const letterSpacing = this.letterSpacing();
        const align = this.align();
        const kerningFunc = this.kerningFunc();
        const textWidth = Math.max(this.textWidth + ((this.attrs.text || '').length - 1) * letterSpacing, 0);
        let offset = 0;
        if (align === 'center') {
            offset = Math.max(0, this.pathLength / 2 - textWidth / 2);
        }
        if (align === 'right') {
            offset = Math.max(0, this.pathLength - textWidth);
        }
        let offsetToGlyph = offset;
        for (let i = 0; i < chars.length; i++) {
            const charStartPoint = this._getPointAtLength(offsetToGlyph);
            if (!charStartPoint)
                return;
            const char = chars[i].char;
            let glyphWidth = chars[i].width + letterSpacing;
            if (char === ' ' && align === 'justify') {
                const numberOfSpaces = this.text().split(' ').length - 1;
                glyphWidth += (this.pathLength - textWidth) / numberOfSpaces;
            }
            const charEndPoint = this._getPointAtLength(offsetToGlyph + glyphWidth);
            if (!charEndPoint) {
                return;
            }
            const width = Path.getLineLength(charStartPoint.x, charStartPoint.y, charEndPoint.x, charEndPoint.y);
            let kern = 0;
            if (kerningFunc) {
                try {
                    kern = kerningFunc(chars[i - 1].char, char) * this.fontSize();
                }
                catch (e) {
                    kern = 0;
                }
            }
            charStartPoint.x += kern;
            charEndPoint.x += kern;
            this.textWidth += kern;
            const midpoint = Path.getPointOnLine(kern + width / 2.0, charStartPoint.x, charStartPoint.y, charEndPoint.x, charEndPoint.y);
            const rotation = Math.atan2(charEndPoint.y - charStartPoint.y, charEndPoint.x - charStartPoint.x);
            this.glyphInfo.push({
                transposeX: midpoint.x,
                transposeY: midpoint.y,
                text: charArr[i],
                rotation: rotation,
                p0: charStartPoint,
                p1: charEndPoint,
                width: width,
            });
            offsetToGlyph += glyphWidth;
        }
    }
    getSelfRect() {
        if (!this.glyphInfo.length) {
            return {
                x: 0,
                y: 0,
                width: 0,
                height: 0,
            };
        }
        const points = [];
        this.glyphInfo.forEach(function (info) {
            points.push(info.p0.x);
            points.push(info.p0.y);
            points.push(info.p1.x);
            points.push(info.p1.y);
        });
        let minX = points[0] || 0;
        let maxX = points[0] || 0;
        let minY = points[1] || 0;
        let maxY = points[1] || 0;
        let x, y;
        for (let i = 0; i < points.length / 2; i++) {
            x = points[i * 2];
            y = points[i * 2 + 1];
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        const fontSize = this.fontSize();
        return {
            x: minX - fontSize / 2,
            y: minY - fontSize / 2,
            width: maxX - minX + fontSize,
            height: maxY - minY + fontSize,
        };
    }
    destroy() {
        Util.releaseCanvas(this.dummyCanvas);
        return super.destroy();
    }
}
TextPath.prototype._fillFunc = _fillFunc;
TextPath.prototype._strokeFunc = _strokeFunc;
TextPath.prototype._fillFuncHit = _fillFunc;
TextPath.prototype._strokeFuncHit = _strokeFunc;
TextPath.prototype.className = 'TextPath';
TextPath.prototype._attrsAffectingSize = ['text', 'fontSize', 'data'];
_registerNode(TextPath);
Factory.addGetterSetter(TextPath, 'data');
Factory.addGetterSetter(TextPath, 'fontFamily', 'Arial');
Factory.addGetterSetter(TextPath, 'fontSize', 12, getNumberValidator());
Factory.addGetterSetter(TextPath, 'fontStyle', NORMAL);
Factory.addGetterSetter(TextPath, 'align', 'left');
Factory.addGetterSetter(TextPath, 'letterSpacing', 0, getNumberValidator());
Factory.addGetterSetter(TextPath, 'textBaseline', 'middle');
Factory.addGetterSetter(TextPath, 'fontVariant', NORMAL);
Factory.addGetterSetter(TextPath, 'text', EMPTY_STRING);
Factory.addGetterSetter(TextPath, 'textDecoration', '');
Factory.addGetterSetter(TextPath, 'kerningFunc', undefined);
