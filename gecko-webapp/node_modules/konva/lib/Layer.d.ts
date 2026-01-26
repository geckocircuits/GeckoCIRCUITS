import type { ContainerConfig } from './Container.ts';
import { Container } from './Container.ts';
import { Node } from './Node.ts';
import { SceneCanvas, HitCanvas } from './Canvas.ts';
import type { Stage } from './Stage.ts';
import type { GetSet, Vector2d } from './types.ts';
import type { Group } from './Group.ts';
import type { Shape } from './Shape.ts';
export interface LayerConfig extends ContainerConfig {
    clearBeforeDraw?: boolean;
    hitGraphEnabled?: boolean;
    imageSmoothingEnabled?: boolean;
}
export declare class Layer extends Container<Group | Shape> {
    canvas: SceneCanvas;
    hitCanvas: HitCanvas;
    _waitingForDraw: boolean;
    constructor(config?: LayerConfig);
    createPNGStream(): any;
    getCanvas(): SceneCanvas;
    getNativeCanvasElement(): HTMLCanvasElement;
    getHitCanvas(): HitCanvas;
    getContext(): import("./Context.ts").Context;
    clear(bounds?: any): this;
    setZIndex(index: number): this;
    moveToTop(): boolean;
    moveUp(): boolean;
    moveDown(): boolean;
    moveToBottom(): boolean;
    getLayer(): this;
    remove(): this;
    getStage(): Stage;
    setSize({ width, height }: {
        width: any;
        height: any;
    }): this;
    _validateAdd(child: any): void;
    _toKonvaCanvas(config: any): SceneCanvas;
    _checkVisibility(): void;
    _setSmoothEnabled(): void;
    getWidth(): number | undefined;
    setWidth(): void;
    getHeight(): number | undefined;
    setHeight(): void;
    batchDraw(): this;
    getIntersection(pos: Vector2d): Shape<import("./Shape.ts").ShapeConfig> | null;
    _getIntersection(pos: Vector2d): {
        shape?: Shape;
        antialiased?: boolean;
    };
    drawScene(can?: SceneCanvas, top?: Node, bufferCanvas?: SceneCanvas): this;
    drawHit(can?: HitCanvas, top?: Node): this;
    enableHitGraph(): this;
    disableHitGraph(): this;
    setHitGraphEnabled(val: any): void;
    getHitGraphEnabled(val: any): boolean;
    toggleHitCanvas(): void;
    destroy(): this;
    hitGraphEnabled: GetSet<boolean, this>;
    clearBeforeDraw: GetSet<boolean, this>;
    imageSmoothingEnabled: GetSet<boolean, this>;
}
