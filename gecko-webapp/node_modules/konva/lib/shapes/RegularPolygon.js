import { Factory } from "../Factory.js";
import { Shape } from "../Shape.js";
import { getNumberOrArrayOfNumbersValidator, getNumberValidator, } from "../Validators.js";
import { _registerNode } from "../Global.js";
import { Util } from "../Util.js";
export class RegularPolygon extends Shape {
    _sceneFunc(context) {
        const points = this._getPoints(), radius = this.radius(), sides = this.sides(), cornerRadius = this.cornerRadius();
        context.beginPath();
        if (!cornerRadius) {
            context.moveTo(points[0].x, points[0].y);
            for (let n = 1; n < points.length; n++) {
                context.lineTo(points[n].x, points[n].y);
            }
        }
        else {
            Util.drawRoundedPolygonPath(context, points, sides, radius, cornerRadius);
        }
        context.closePath();
        context.fillStrokeShape(this);
    }
    _getPoints() {
        const sides = this.attrs.sides;
        const radius = this.attrs.radius || 0;
        const points = [];
        for (let n = 0; n < sides; n++) {
            points.push({
                x: radius * Math.sin((n * 2 * Math.PI) / sides),
                y: -1 * radius * Math.cos((n * 2 * Math.PI) / sides),
            });
        }
        return points;
    }
    getSelfRect() {
        const points = this._getPoints();
        let minX = points[0].x;
        let maxX = points[0].x;
        let minY = points[0].y;
        let maxY = points[0].y;
        points.forEach((point) => {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        });
        return {
            x: minX,
            y: minY,
            width: maxX - minX,
            height: maxY - minY,
        };
    }
    getWidth() {
        return this.radius() * 2;
    }
    getHeight() {
        return this.radius() * 2;
    }
    setWidth(width) {
        this.radius(width / 2);
    }
    setHeight(height) {
        this.radius(height / 2);
    }
}
RegularPolygon.prototype.className = 'RegularPolygon';
RegularPolygon.prototype._centroid = true;
RegularPolygon.prototype._attrsAffectingSize = ['radius'];
_registerNode(RegularPolygon);
Factory.addGetterSetter(RegularPolygon, 'radius', 0, getNumberValidator());
Factory.addGetterSetter(RegularPolygon, 'sides', 0, getNumberValidator());
Factory.addGetterSetter(RegularPolygon, 'cornerRadius', 0, getNumberOrArrayOfNumbersValidator(4));
