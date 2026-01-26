import type { ShapeConfig } from '../Shape.ts';
import { Shape } from '../Shape.ts';
import type { GetSet, Vector2d } from '../types.ts';
import type { Context } from '../Context.ts';
export interface RegularPolygonConfig extends ShapeConfig {
    sides: number;
    radius: number;
    cornerRadius?: number | number[];
}
export declare class RegularPolygon extends Shape<RegularPolygonConfig> {
    _sceneFunc(context: Context): void;
    _getPoints(): Vector2d[];
    getSelfRect(): {
        x: number;
        y: number;
        width: number;
        height: number;
    };
    getWidth(): number;
    getHeight(): number;
    setWidth(width: number): void;
    setHeight(height: number): void;
    radius: GetSet<number, this>;
    sides: GetSet<number, this>;
    cornerRadius: GetSet<number | number[], this>;
}
