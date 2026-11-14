import os
from PIL import Image, ImageDraw, ImageFont

def render_logo(output_size: int, output_path: str):
    # Supersample at 4x for crisp anti-aliasing
    scale = 4
    canvas_size = output_size * scale
    img = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    def s(val):
        return int(round(val * canvas_size / 512.0))

    # 1. Background Squircle
    # Radius = 116 in 512 grid
    r = s(116)
    # Emerald gradient simulation / rich emerald base
    # Fill rounded rectangle
    draw.rounded_rectangle(
        [0, 0, canvas_size - 1, canvas_size - 1],
        radius=r,
        fill=(15, 81, 50, 255) # Royal Emerald #0F5132
    )

    # Gradient overlay (darker bottom)
    gradient_overlay = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    grad_draw = ImageDraw.Draw(gradient_overlay)
    for y in range(canvas_size):
        alpha = int(70 * (y / canvas_size))
        grad_draw.line([(0, y), (canvas_size, y)], fill=(10, 54, 34, alpha))
    img.alpha_composite(gradient_overlay)

    # 2. House Structure (White silhouette with roof gables)
    house_pts = [
        (s(256), s(100)),
        (s(126), s(215)),
        (s(130), s(230)),
        (s(152), s(230)),
        (s(152), s(398)),
        (s(360), s(398)),
        (s(360), s(230)),
        (s(382), s(230)),
        (s(386), s(215)),
    ]
    # Draw drop shadow for house
    shadow = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    shadow_offset = s(6)
    shadow_pts = [(x, y + shadow_offset) for x, y in house_pts]
    s_draw.polygon(shadow_pts, fill=(0, 0, 0, 50))
    img.alpha_composite(shadow)

    # Draw white house
    draw = ImageDraw.Draw(img)
    draw.polygon(house_pts, fill=(255, 255, 255, 255))

    # 3. Arched Doorway in Deep Emerald
    door_rect = [s(212), s(305), s(300), s(398)]
    draw.rectangle(door_rect, fill=(13, 75, 41, 255))
    # Top arch of door
    arch_bbox = [s(212), s(260), s(300), s(350)]
    draw.chord(arch_bbox, start=180, end=360, fill=(13, 75, 41, 255))

    # 4. Golden Coin Medallion
    cx, cy = s(256), s(264)
    cr = s(58)
    # Coin shadow
    coin_shadow = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    cs_draw = ImageDraw.Draw(coin_shadow)
    cs_draw.ellipse([cx - cr, cy - cr + s(6), cx + cr, cy + cr + s(6)], fill=(0, 0, 0, 60))
    img.alpha_composite(coin_shadow)

    draw = ImageDraw.Draw(img)
    # Coin outer border
    draw.ellipse([cx - cr, cy - cr, cx + cr, cy + cr], fill=(217, 119, 6, 255)) # #D97706
    # Coin inner face
    draw.ellipse([cx - cr + s(4), cy - cr + s(4), cx + cr - s(4), cy + cr - s(4)], fill=(245, 158, 11, 255)) # #F59E0B
    # Inner rim dashed line simulation
    draw.ellipse([cx - cr + s(10), cy - cr + s(10), cx + cr - s(10), cy + cr - s(10)], outline=(254, 243, 199, 220), width=max(1, s(2)))

    # 5. Crisp Rupee Symbol (₹) on coin in pure white
    # Top horizontal bar
    draw.rounded_rectangle([s(236), s(235), s(276), s(242)], radius=s(3), fill=(255, 255, 255, 255))
    # Second horizontal bar
    draw.rounded_rectangle([s(236), s(248), s(273), s(255)], radius=s(3), fill=(255, 255, 255, 255))
    # Vertical spine
    draw.rounded_rectangle([s(240), s(235), s(248), s(293)], radius=s(2), fill=(255, 255, 255, 255))
    # Upper loop of R
    loop_bbox = [s(240), s(242), s(272), s(270)]
    draw.arc(loop_bbox, start=270, end=90, fill=(255, 255, 255, 255), width=s(7))
    # Diagonal leg
    leg_pts = [
        (s(248), s(266)),
        (s(276), s(293)),
        (s(284), s(293)),
        (s(256), s(266))
    ]
    draw.polygon(leg_pts, fill=(255, 255, 255, 255))

    # Resize down with Lanczos filter for exceptional clarity
    final_img = img.resize((output_size, output_size), Image.Resampling.LANCZOS)
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    final_img.save(output_path, "PNG")
    print(f"Generated: {output_path} ({output_size}x{output_size})")

if __name__ == "__main__":
    render_logo(512, "docs/brand/logo-512.png")
    render_logo(1024, "docs/brand/logo-1024.png")

