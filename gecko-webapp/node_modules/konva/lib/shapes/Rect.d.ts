import type { ShapeConfig } from '../Shape.ts';
import { Shape } from '../Shape.ts';
import type { GetSet } from '../types.ts';
import type { Context } from '../Context.ts';
export type RectConfig = ShapeConfig & {
    cornerRadius?: number | number[];
};
export declare class Rect extends Shape<RectConfig> {
    _sceneFunc(context: Context): void;
    cornerRadius: GetSet<number | number[], this>;
}
