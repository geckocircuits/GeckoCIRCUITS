export interface GetSet<Type, This> {
    (): Type;
    (v: Type | null | undefined): This;
}
export interface Vector2d {
    x: number;
    y: number;
}
export interface PathSegment {
    command: 'm' | 'M' | 'l' | 'L' | 'v' | 'V' | 'h' | 'H' | 'z' | 'Z' | 'c' | 'C' | 'q' | 'Q' | 't' | 'T' | 's' | 'S' | 'a' | 'A';
    start: Vector2d;
    points: number[];
    pathLength: number;
}
export interface IRect {
    x: number;
    y: number;
    width: number;
    height: number;
}
export interface IFrame {
    time: number;
    timeDiff: number;
    lastTime: number;
    frameRate: number;
}
export type AnimationFn = (frame: IFrame) => boolean | void;
export declare const KonvaNodeEvent: {
    readonly mouseover: "mouseover";
    readonly mouseout: "mouseout";
    readonly mousemove: "mousemove";
    readonly mouseleave: "mouseleave";
    readonly mouseenter: "mouseenter";
    readonly mousedown: "mousedown";
    readonly mouseup: "mouseup";
    readonly wheel: "wheel";
    readonly contextmenu: "contextmenu";
    readonly click: "click";
    readonly dblclick: "dblclick";
    readonly touchstart: "touchstart";
    readonly touchmove: "touchmove";
    readonly touchend: "touchend";
    readonly tap: "tap";
    readonly dbltap: "dbltap";
    readonly dragstart: "dragstart";
    readonly dragmove: "dragmove";
    readonly dragend: "dragend";
};
export interface RGB {
    r: number;
    g: number;
    b: number;
}
export interface RGBA extends RGB {
    a: number;
}
