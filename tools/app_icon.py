import math
import os

frame_size = 512


def summon_path(d: str, attr: str, rot: float, other_ele: str = ""):
    return f"""<g transform="rotate({rot}, {frame_size/2}, {frame_size/2})"><path d="{d}" {attr} />{other_ele}</g>"""


def summon(size: tuple[float, float], ele: list[str]):
    """
    将路径数据拼接为svg文件字符串
    :param size: svg大小
    :param paths: 元素
    """
    return f'''<svg xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="none" viewBox="0 0 {frame_size} {frame_size}" width="{size[0]}" height="{size[1]}"><defs><linearGradient id="fill_color" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="{frame_size}" y2="{frame_size}"><stop offset="0%" stop-color="#fff" /><stop offset="100%" stop-color="#ffc4ff" /></linearGradient></defs>{"".join(ele)}</svg>'''


def generate_path_line(points: list[tuple[float, float]], z=False):
    """
    生成直线连接的路径
    :param points: 所有点
    :return: 一个path的路径描述
    """
    path_data = ""  # 初始化路径数据

    if len(points) > 0:
        path_data += f"M{points[0][0]},{points[0][1]}"  # 移动到第一个点

        for point in points[1:]:
            path_data += f"L{point[0]},{point[1]}"  # 连接到后续点的位置
        if z:
            path_data += 'Z'

    return path_data


def generate_controller(ctrl: list[float | tuple[float, float]], pts: list[tuple[float, float]], r: float, is_radian=False, is_opposite=True):
    """
    生成控制点
    :param ctrl: 所有控制点的角度
    :param pts: 基准点, ctrl数量应为 (len(pts)-2)*2+2
    :param r: 控制点距离基准点距离
    :param is_radian: 传入的控制点角度是否为弧度制(默认为False)
    :param is_opposite: 传入的控制点角度是否为相反数(默认为True)
    :return: 生成的一组控制点
    """
    pts = [val for pair in zip(pts, pts[1:]) for val in pair]
    assert (len(pts) == len(ctrl))
    rs = [r for _ in ctrl]
    rs = [c[1] if isinstance(c, tuple) else r for r, c in zip(rs, ctrl)]
    ctrl = [c[0] if isinstance(c, tuple) else c for c in ctrl]
    if is_opposite:
        ctrl = [-a for a in ctrl]
    if not is_radian:
        ctrl = [math.radians(d) for d in ctrl]
    ctrl_pt = [(p[0]+r*math.cos(a), p[1]+r*math.sin(a))
               for p, a, r in zip(pts, ctrl, rs)]
    return ctrl_pt


def generate_path_cubic_bezier_curve(points: list[tuple[float, float]], controller: list[tuple[float, float]] | None = None):
    """
    生成三次贝塞尔曲线
    :param points: 关键点
    :param controller: 控制点
    """
    if controller is None:
        return generate_path_line(points, z=True)
    path_data = ""  # 初始化路径数据
    def p_str(p): return f"{p[0]},{p[1]}"
    path_data += f"M{p_str(points.pop(0))} "
    while len(points) > 0:
        path_data += f"C{p_str(controller.pop(0))} "
        path_data += f"{p_str(controller.pop(0))} "
        path_data += f"{p_str(points.pop(0))} "
    path_data += 'Z'
    return path_data


def generate_path_smooth_line(points):
    """
    生成平滑曲线(使用三次贝瑟尔曲线)
    """
    path_data = ""  # 初始化路径数据

    if len(points) > 0:
        path_data += f"M{points[0][0]},{points[0][1]}"

        if len(points) == 2:
            path_data += f"L{points[1][0]},{points[1][1]}"
        else:
            def calculate_control_point(points, index):
                x0, y0 = points[index-1]
                x1, y1 = points[index]
                x2, y2 = points[index+1]

                control_point1 = ((2*x1+x0)/3, (2*y1+y0)/3)
                control_point2 = ((2*x1+x2)/3, (2*y1+y2)/3)

                return control_point1, control_point2
            for i in range(1, len(points)-1):
                control_point = calculate_control_point(points, i)
                path_data += f"C{control_point[0][0]},{control_point[0][1]} {control_point[1][0]},{control_point[1][1]} {points[i][0]},{points[i][1]}"

            path_data += f"L{points[-1][0]},{points[-1][1]}"

    return path_data


def get_path_center() -> str:
    """
    生成中心图案
    """
    s2 = math.sqrt(2)
    center = (frame_size/2, frame_size/2)
    pa_len = frame_size/4
    pb_len = s2 * frame_size*5/64
    dirs_pa: list[tuple[float, float]] = [
        (0, -1), (-1, 0), (0, 1), (1, 0), (0, -1)]
    dirs_pb: list[tuple[float, float]] = [
        (-1, -1), (-1, 1), (1, 1), (1, -1)]
    pa = [(pa_len*d[0]+center[0], pa_len*d[1]+center[1]) for d in dirs_pa]
    pb = [(pb_len*d[0]+center[0], pb_len*d[1]+center[1]) for d in dirs_pb]
    points = [val for pair in zip(pa, pb)
              for val in pair] + pa[len(pb):] + pb[len(pa):]
    controller_angle = [180, 45, -135, 90, 270, 135, -
                        45, 180, 0, -135, 45, 270, 90, -45, 135, 0]
    controller = generate_controller(controller_angle, points, frame_size/16)
    return summon_path(generate_path_cubic_bezier_curve(points, controller), 'fill="#000000" stroke="#000000" stroke-width="5" stroke-dasharray="none"  stroke-dashoffset="0" stroke-linecap="butt" stroke-linejoin="miter" stroke-miterlimit="4"', 0) + f'<circle cx="{frame_size/2}" cy="{frame_size/2}" r="{frame_size/8}" fill="url(#fill_color)" stroke="#fff" />' + f'<circle cx="{frame_size/2-frame_size/32}" cy="{frame_size/2}" r="{frame_size/36}" fill="#ff7c07"/>' + f'<circle cx="{frame_size/2+frame_size/32}" cy="{frame_size/2}" r="{frame_size/36}" fill="#168bf4"/>'


def get_path_stick(a: float):
    s2 = math.sqrt(2)
    stick_w = frame_size/50
    stick_start, stick_end = s2 * frame_size*7/64,  s2 * frame_size*13/64
    pts = [
        (stick_start+frame_size/2, (frame_size-stick_w)/2),
        (stick_end+frame_size/2, (frame_size-stick_w)/2),
        (stick_end+frame_size/2, (frame_size+stick_w)/2),
        (stick_start+frame_size/2, (frame_size+stick_w)/2)
    ]
    return summon_path(generate_path_line(pts, z=True), 'fill="#000000" stroke="#000000" stroke-width="5" stroke-dasharray="none"  stroke-dashoffset="0" stroke-linecap="butt" stroke-linejoin="miter" stroke-miterlimit="4"', a)


def get_path_head(a: float, color_i):
    s2 = math.sqrt(2)
    ws = [frame_size/50, frame_size*8/100, frame_size*15/100, frame_size/50]
    hs = [s2 * frame_size*26/128, s2 * frame_size*24/128,
          s2 * frame_size*28/128, s2 * frame_size*33/128,]
    pt = [(w/2, frame_size/2+h)for w, h in zip(ws, hs)]
    pt1 = [(frame_size/2-x, y) for x, y in pt]
    pt2 = [(frame_size/2+x, y) for x, y in pt][::-1]
    pts = pt1+pt2

    r = frame_size/512
    angle = [90, 0, (180, r*20), 90, (-90, r*16), 180, 0,
             180, 0,  (-90, r*16), 90, (0, r*20), 180, 90]
    controller = generate_controller(angle, pts, r*8)

    color = ['16b2ff', 'ffde09', '1abcff', 'ff8706']
    color: str = f"#{color[(color_i+len(color)-1)%len(color)]}"
    core = f'<circle cx="{frame_size/2}" cy="{frame_size/2+hs[2]}" r="{frame_size/36}" fill="{color}"/>'

    return summon_path(generate_path_cubic_bezier_curve(pts, controller), 'fill="#000000" stroke="#000000" stroke-width="5" stroke-dasharray="none"  stroke-dashoffset="0" stroke-linecap="butt" stroke-linejoin="miter" stroke-miterlimit="4"', a, other_ele=core)


def get_path_bg():
    return f'<rect x="0" y="0" width="{frame_size}" height="{frame_size}" fill="url(#fill_color)" />'


def get_path():
    data: list[str] = []
    data.append(get_path_center())
    for i in range(4):
        a = i*90+45
        data.append(get_path_stick(a))
    for i in range(4):
        a = i*90+45
        data.append(get_path_head(a, i))
    return data


if __name__ == "__main__":
    dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'icons')
    os.makedirs(dir, exist_ok=True)

    with open(os.path.join(dir, 'icon.fg.svg'), 'w') as f:
        f.write(summon((500, 500), get_path()))
    with open(os.path.join(dir, 'icon.bg.svg'), 'w') as f:
        f.write(summon((500, 500), get_path_bg()))
