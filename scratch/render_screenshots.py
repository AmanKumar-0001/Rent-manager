import os
from PIL import Image, ImageDraw, ImageFont

WIDTH = 840
HEIGHT = 1840

FONT_DIR = "C:/Windows/Fonts"
font_reg_path = os.path.join(FONT_DIR, "segoeui.ttf")
font_bold_path = os.path.join(FONT_DIR, "segoeuib.ttf")
font_semi_path = os.path.join(FONT_DIR, "segoeuisb.ttf") if os.path.exists(os.path.join(FONT_DIR, "segoeuisb.ttf")) else font_bold_path

def f(size, bold=False):
    path = font_bold_path if bold else font_reg_path
    try:
        return ImageFont.truetype(path, size)
    except Exception:
        return ImageFont.load_default()

def draw_status_bar(draw, is_dark, bg_color):
    fg = (240, 240, 240) if is_dark else (30, 30, 30)
    draw.text((48, 20), "9:41", font=f(28, True), fill=fg)
    # Icons: cell, wifi, battery
    cx = WIDTH - 140
    # Wifi arc / bar
    draw.arc([cx, 24, cx + 24, 48], 180, 360, fill=fg, width=3)
    # Battery
    bx = cx + 45
    draw.rectangle([bx, 26, bx + 42, 46], outline=fg, width=3)
    draw.rectangle([bx + 4, 30, bx + 32, 42], fill=fg)
    draw.rectangle([bx + 42, 32, bx + 46, 40], fill=fg)

def draw_nav_bar(draw, is_dark, active_tab="Dashboard"):
    bg = (24, 28, 32) if is_dark else (245, 247, 245)
    border_col = (45, 52, 60) if is_dark else (225, 230, 226)
    draw.rectangle([0, HEIGHT - 130, WIDTH, HEIGHT], fill=bg)
    draw.line([0, HEIGHT - 130, WIDTH, HEIGHT - 130], fill=border_col, width=2)

    tabs = [("Dashboard", 0), ("Tenants", 1), ("Payments", 2), ("Settings", 3)]
    tab_w = WIDTH // 4
    for name, idx in tabs:
        center_x = idx * tab_w + tab_w // 2
        is_active = (name == active_tab)
        tint = (38, 166, 91) if is_active else ((160, 165, 170) if is_dark else (120, 125, 130))
        # Indicator pill for active
        if is_active:
            pill_col = (20, 60, 40) if is_dark else (215, 245, 225)
            draw.rounded_rectangle([center_x - 48, HEIGHT - 118, center_x + 48, HEIGHT - 76], radius=20, fill=pill_col)
        # Tab icon dot / symbol
        draw.ellipse([center_x - 8, HEIGHT - 104, center_x + 8, HEIGHT - 88], fill=tint)
        # Label
        bbox = draw.textbbox((0, 0), name, font=f(22, is_active))
        w = bbox[2] - bbox[0]
        draw.text((center_x - w // 2, HEIGHT - 64), name, font=f(22, is_active), fill=tint)

def draw_top_bar(draw, title, is_dark, action_text=None):
    fg = (245, 245, 245) if is_dark else (20, 24, 22)
    draw.text((48, 88), title, font=f(40, True), fill=fg)
    if action_text:
        bbox = draw.textbbox((0, 0), action_text, font=f(26, True))
        w = bbox[2] - bbox[0]
        draw.text((WIDTH - 48 - w, 96), action_text, font=f(26, True), fill=(38, 166, 91))

def draw_card(draw, x1, y1, x2, y2, radius, fill_col, border_col=None):
    draw.rounded_rectangle([x1, y1, x2, y2], radius=radius, fill=fill_col, outline=border_col, width=2 if border_col else 0)

def draw_status_chip(draw, x, y, label, is_dark, status_type):
    # status_type: 'paid', 'due_soon', 'overdue', 'left'
    if status_type == 'paid':
        bg = (18, 55, 35) if is_dark else (220, 245, 230)
        fg = (74, 222, 128) if is_dark else (15, 81, 50)
        icon_sym = "✓"
    elif status_type == 'due_soon':
        bg = (55, 45, 15) if is_dark else (254, 243, 199)
        fg = (250, 204, 21) if is_dark else (146, 64, 14)
        icon_sym = "⏳"
    elif status_type == 'overdue':
        bg = (60, 20, 20) if is_dark else (254, 226, 226)
        fg = (248, 113, 113) if is_dark else (153, 27, 27)
        icon_sym = "!"
    else:
        bg = (40, 40, 45) if is_dark else (240, 240, 245)
        fg = (180, 180, 190) if is_dark else (70, 75, 85)
        icon_sym = "←"

    text = f"{icon_sym}  {label}"
    bbox = draw.textbbox((0, 0), text, font=f(22, True))
    w = (bbox[2] - bbox[0]) + 28
    h = 42
    draw.rounded_rectangle([x, y, x + w, y + h], radius=10, fill=bg)
    draw.text((x + 14, y + 8), text, font=f(22, True), fill=fg)
    return w

# ----------------- SCREENS AFTER (PHASE 3) -----------------

def render_dashboard_after(is_dark):
    im = Image.new("RGB", (WIDTH, HEIGHT), (16, 20, 24) if is_dark else (248, 250, 248))
    d = ImageDraw.Draw(im)
    draw_status_bar(d, is_dark, im)
    draw_top_bar(d, "Dashboard", is_dark)

    card_bg = (28, 34, 40) if is_dark else (255, 255, 255)
    border = (45, 52, 60) if is_dark else (230, 234, 230)
    text_main = (245, 245, 245) if is_dark else (20, 24, 22)
    text_sub = (160, 165, 170) if is_dark else (100, 105, 110)

    # 1. Executive Rent Collection Hero Card (Emerald)
    hero_bg = (13, 65, 42) if is_dark else (15, 81, 50)
    draw_card(d, 40, 160, WIDTH - 40, 430, 24, hero_bg)
    d.text((68, 185), "November 2026 Collection", font=f(24, False), fill=(210, 235, 220))
    d.text((68, 220), "₹1,45,000", font=f(54, True), fill=(255, 255, 255))
    d.text((360, 242), "collected", font=f(26, False), fill=(210, 235, 220))

    # Progress Bar
    draw_card(d, 68, 305, WIDTH - 68, 323, 9, (30, 95, 65))
    progress_w = int((WIDTH - 136) * 0.76)
    draw_card(d, 68, 305, 68 + progress_w, 323, 9, (74, 222, 128))

    d.text((68, 345), "76% of ₹1,90,000 expected", font=f(24, True), fill=(255, 255, 255))
    d.text((WIDTH - 250, 345), "₹45,000 pending", font=f(24, False), fill=(240, 210, 180))

    # 2. Attention Required Section
    d.text((40, 465), "ATTENTION REQUIRED (1)", font=f(24, True), fill=(239, 68, 68))
    draw_card(d, 40, 505, WIDTH - 40, 685, 20, (45, 22, 22) if is_dark else (254, 242, 242), (180, 50, 50) if is_dark else (252, 165, 165))
    # Tenant: Aarav Sharma
    d.text((68, 528), "Aarav Sharma", font=f(32, True), fill=text_main)
    d.text((68, 570), "Unit 301 • Sunshine Heights", font=f(24, False), fill=text_sub)
    draw_status_chip(d, WIDTH - 240, 528, "OVERDUE", is_dark, "overdue")

    d.text((68, 618), "Overdue by 5 days • Rent: ₹15,000", font=f(24, True), fill=(239, 68, 68))
    # Pay button
    draw_card(d, WIDTH - 200, 610, WIDTH - 68, 660, 12, (38, 166, 91))
    d.text((WIDTH - 165, 622), "Pay", font=f(24, True), fill=(255, 255, 255))

    # 3. Upcoming Dues Section
    d.text((40, 725), "UPCOMING DUES (2)", font=f(24, True), fill=text_sub)
    # Tenant 1: Rahul Verma
    draw_card(d, 40, 765, WIDTH - 40, 945, 20, card_bg, border)
    d.text((68, 788), "Rahul Verma", font=f(32, True), fill=text_main)
    d.text((68, 830), "Unit 102 • Sunshine Heights", font=f(24, False), fill=text_sub)
    draw_status_chip(d, WIDTH - 250, 788, "DUE SOON", is_dark, "due_soon")
    d.text((68, 878), "Due: 18 Nov 2026 • Rent: ₹22,000", font=f(24, False), fill=text_main)
    draw_card(d, WIDTH - 200, 870, WIDTH - 68, 920, 12, card_bg, (38, 166, 91))
    d.text((WIDTH - 165, 882), "Pay", font=f(24, True), fill=(38, 166, 91))

    # Tenant 2: Priya Patel
    draw_card(d, 40, 970, WIDTH - 40, 1150, 20, card_bg, border)
    d.text((68, 993), "Priya Patel", font=f(32, True), fill=text_main)
    d.text((68, 1035), "Unit 204 • Palm Residency", font=f(24, False), fill=text_sub)
    draw_status_chip(d, WIDTH - 250, 993, "DUE SOON", is_dark, "due_soon")
    d.text((68, 1083), "Due: 20 Nov 2026 • Rent: ₹18,000", font=f(24, False), fill=text_main)
    draw_card(d, WIDTH - 200, 1075, WIDTH - 68, 1125, 12, card_bg, (38, 166, 91))
    d.text((WIDTH - 165, 1087), "Pay", font=f(24, True), fill=(38, 166, 91))

    # 4. Paid Tenants Summary Section
    d.text((40, 1190), "RECENTLY COLLECTED", font=f(24, True), fill=text_sub)
    draw_card(d, 40, 1230, WIDTH - 40, 1410, 20, card_bg, border)
    d.text((68, 1253), "Deepak Nair", font=f(32, True), fill=text_main)
    d.text((68, 1295), "Unit 501 • Palm Residency", font=f(24, False), fill=text_sub)
    draw_status_chip(d, WIDTH - 210, 1253, "PAID", is_dark, "paid")
    d.text((68, 1343), "Paid ₹28,000 on 05 Nov 2026", font=f(24, False), fill=(38, 166, 91))

    draw_nav_bar(d, is_dark, "Dashboard")
    return im

def render_tenants_list_after(is_dark):
    im = Image.new("RGB", (WIDTH, HEIGHT), (16, 20, 24) if is_dark else (248, 250, 248))
    d = ImageDraw.Draw(im)
    draw_status_bar(d, is_dark, im)
    draw_top_bar(d, "Tenants", is_dark, action_text="+ Add Tenant")

    card_bg = (28, 34, 40) if is_dark else (255, 255, 255)
    border = (45, 52, 60) if is_dark else (230, 234, 230)
    text_main = (245, 245, 245) if is_dark else (20, 24, 22)
    text_sub = (160, 165, 170) if is_dark else (100, 105, 110)

    # Search Bar
    draw_card(d, 40, 155, WIDTH - 40, 225, 16, card_bg, border)
    d.text((68, 173), "🔍   Search by name, unit, or property...", font=f(24, False), fill=text_sub)

    # Filter Chips
    chips = ["All (6)", "Overdue (1)", "Due Soon (2)", "Paid (3)", "Left (1)"]
    cx = 40
    for i, c in enumerate(chips):
        is_sel = (i == 0)
        bg = (38, 166, 91) if is_sel else ((36, 44, 52) if is_dark else (235, 238, 235))
        fg = (255, 255, 255) if is_sel else text_main
        bbox = d.textbbox((0, 0), c, font=f(22, is_sel))
        cw = (bbox[2] - bbox[0]) + 30
        draw_card(d, cx, 245, cx + cw, 295, 14, bg)
        d.text((cx + 15, 257), c, font=f(22, is_sel), fill=fg)
        cx += cw + 14

    # Tenant Cards (Urgency Ordered: Overdue -> Due Soon -> Paid)
    tenants = [
        ("Aarav Sharma", "A", "Unit 301 • Sunshine Heights", "₹15,000", "05 Nov 2026", "OVERDUE", "overdue", True),
        ("Rahul Verma", "R", "Unit 102 • Sunshine Heights", "₹22,000", "18 Nov 2026", "DUE SOON", "due_soon", False),
        ("Priya Patel", "P", "Unit 204 • Palm Residency", "₹18,000", "20 Nov 2026", "DUE SOON", "due_soon", False),
        ("Deepak Nair", "D", "Unit 501 • Palm Residency", "₹28,000", "05 Dec 2026", "PAID", "paid", False),
        ("Kavita Rao", "K", "Unit 202 • Greenfield Villa", "₹32,000", "01 Dec 2026", "PAID", "paid", False),
    ]

    cy = 320
    for name, init, unit, rent, nxt, st_label, st_type, is_overdue in tenants:
        draw_card(d, 40, cy, WIDTH - 40, cy + 200, 20, card_bg, border)
        # Avatar initial circle
        avatar_bg = (25, 75, 50) if is_dark else (210, 240, 225)
        avatar_fg = (74, 222, 128) if is_dark else (15, 81, 50)
        d.ellipse([64, cy + 24, 134, cy + 94], fill=avatar_bg)
        d.text((86, cy + 38), init, font=f(34, True), fill=avatar_fg)

        # Name and Unit
        d.text((150, cy + 24), name, font=f(32, True), fill=text_main)
        d.text((150, cy + 66), unit, font=f(22, False), fill=text_sub)
        draw_status_chip(d, WIDTH - 240, cy + 24, st_label, is_dark, st_type)

        # Rent and Next Payment
        d.text((64, cy + 115), "Monthly Rent", font=f(20, False), fill=text_sub)
        d.text((64, cy + 142), rent, font=f(30, True), fill=(38, 166, 91))

        d.text((360, cy + 115), "Next Payment Due", font=f(20, False), fill=text_sub)
        d.text((360, cy + 142), nxt, font=f(26, True), fill=(239, 68, 68) if is_overdue else text_main)

        # Action button
        draw_card(d, WIDTH - 160, cy + 120, WIDTH - 64, cy + 175, 12, (38, 166, 91))
        d.text((WIDTH - 130, cy + 134), "Pay", font=f(24, True), fill=(255, 255, 255))

        cy += 225

    draw_nav_bar(d, is_dark, "Tenants")
    return im

def render_tenant_detail_after(is_dark):
    im = Image.new("RGB", (WIDTH, HEIGHT), (16, 20, 24) if is_dark else (248, 250, 248))
    d = ImageDraw.Draw(im)
    draw_status_bar(d, is_dark, im)
    draw_top_bar(d, "Tenant Details", is_dark, action_text="Edit")

    card_bg = (28, 34, 40) if is_dark else (255, 255, 255)
    border = (45, 52, 60) if is_dark else (230, 234, 230)
    text_main = (245, 245, 245) if is_dark else (20, 24, 22)
    text_sub = (160, 165, 170) if is_dark else (100, 105, 110)

    # Hero Profile Card
    draw_card(d, 40, 160, WIDTH - 40, 390, 22, card_bg, border)
    avatar_bg = (25, 75, 50) if is_dark else (210, 240, 225)
    avatar_fg = (74, 222, 128) if is_dark else (15, 81, 50)
    d.ellipse([68, 185, 158, 275], fill=avatar_bg)
    d.text((96, 203), "A", font=f(46, True), fill=avatar_fg)

    d.text((180, 188), "Aarav Sharma", font=f(38, True), fill=text_main)
    d.text((180, 236), "Unit 301 • Sunshine Heights", font=f(24, False), fill=text_sub)
    draw_status_chip(d, WIDTH - 240, 188, "OVERDUE", is_dark, "overdue")

    # Quick contact buttons: Call, SMS, Email
    btn_w = (WIDTH - 136) // 3
    # Call
    draw_card(d, 68, 300, 68 + btn_w, 360, 14, (36, 44, 52) if is_dark else (240, 243, 240))
    d.text((68 + 40, 316), "📞 Call", font=f(24, True), fill=text_main)
    # SMS
    draw_card(d, 68 + btn_w + 14, 300, 68 + btn_w * 2 + 14, 360, 14, (36, 44, 52) if is_dark else (240, 243, 240))
    d.text((68 + btn_w + 54, 316), "💬 SMS", font=f(24, True), fill=text_main)
    # Email
    draw_card(d, 68 + btn_w * 2 + 28, 300, WIDTH - 68, 360, 14, (36, 44, 52) if is_dark else (240, 243, 240))
    d.text((68 + btn_w * 2 + 62, 316), "✉ Email", font=f(24, True), fill=text_main)

    # Next Payment & Balance Card
    draw_card(d, 40, 415, WIDTH - 40, 565, 20, (45, 22, 22) if is_dark else (254, 242, 242), (180, 50, 50) if is_dark else (252, 165, 165))
    d.text((68, 438), "Next Payment Due: 05 Nov 2026", font=f(26, True), fill=(239, 68, 68))
    d.text((68, 475), "Status: 9 days overdue (Grace period ended)", font=f(22, False), fill=text_sub)
    d.text((68, 510), "Outstanding Balance:", font=f(24, False), fill=text_main)
    d.text((320, 506), "₹15,000", font=f(32, True), fill=(239, 68, 68))

    # Agreement Details
    d.text((40, 595), "AGREEMENT DETAILS", font=f(24, True), fill=text_sub)
    draw_card(d, 40, 635, WIDTH - 40, 895, 20, card_bg, border)
    rows = [
        ("Monthly Rent", "₹15,000"),
        ("Due Day", "5th of every month"),
        ("Joined On", "01 Jan 2026"),
        ("Security Deposit", "₹45,000"),
        ("Deposit Returned", "Active (Not Returned)")
    ]
    ry = 655
    for lbl, val in rows:
        d.text((68, ry), lbl, font=f(24, False), fill=text_sub)
        d.text((WIDTH - 68 - d.textbbox((0, 0), val, font=f(24, True))[2], ry), val, font=f(24, True), fill=text_main)
        ry += 46

    # Monthly Rent Ledger
    d.text((40, 925), "MONTHLY RENT LEDGER (11)", font=f(24, True), fill=text_sub)
    ledger_rows = [
        ("Nov 2026", "Due 05 Nov", "₹15,000", "UNPAID", "overdue"),
        ("Oct 2026", "Paid 04 Oct", "₹15,000", "PAID", "paid"),
        ("Sep 2026", "Paid 05 Sep", "₹15,000", "PAID", "paid"),
        ("Aug 2026", "Paid 03 Aug", "₹15,000", "PAID", "paid"),
    ]
    ly = 965
    for period, dt, amt, st_lbl, st_t in ledger_rows:
        draw_card(d, 40, ly, WIDTH - 40, ly + 115, 16, card_bg, border)
        d.text((68, ly + 22), period, font=f(28, True), fill=text_main)
        d.text((68, ly + 64), f"{dt} • Rent: {amt}", font=f(22, False), fill=text_sub)
        draw_status_chip(d, WIDTH - 220, ly + 32, st_lbl, is_dark, st_t)
        ly += 135

    # Sticky Bottom Bar
    draw_card(d, 0, HEIGHT - 140, WIDTH, HEIGHT, 0, (22, 28, 34) if is_dark else (255, 255, 255), border)
    draw_card(d, 48, HEIGHT - 115, WIDTH - 48, HEIGHT - 35, 18, (38, 166, 91))
    d.text((WIDTH // 2 - 120, HEIGHT - 86), "Record Payment", font=f(28, True), fill=(255, 255, 255))
    return im

def render_record_payment_after(is_dark):
    im = render_tenant_detail_after(is_dark)
    # Dim overlay
    dim = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 140))
    im = Image.alpha_composite(im.convert("RGBA"), dim).convert("RGB")
    d = ImageDraw.Draw(im)

    # Bottom Sheet (ModalBottomSheet)
    sheet_bg = (28, 34, 40) if is_dark else (255, 255, 255)
    border = (45, 52, 60) if is_dark else (225, 230, 225)
    text_main = (245, 245, 245) if is_dark else (20, 24, 22)
    text_sub = (160, 165, 170) if is_dark else (100, 105, 110)

    sy = 650
    draw_card(d, 0, sy, WIDTH, HEIGHT, 28, sheet_bg, border)
    # Handle bar
    draw_card(d, WIDTH // 2 - 40, sy + 18, WIDTH // 2 + 40, sy + 26, 4, (100, 105, 110))

    d.text((48, sy + 55), "Record Rent Payment", font=f(36, True), fill=text_main)
    d.text((48, sy + 105), "Aarav Sharma • Unit 301", font=f(24, False), fill=text_sub)

    # Quick Select Chips
    chips = ["Full Rent (₹15,000)", "Remaining (₹15,000)", "₹5,000", "₹10,000"]
    cx = 48
    for i, c in enumerate(chips):
        is_sel = (i == 0)
        bg = (38, 166, 91) if is_sel else ((38, 46, 54) if is_dark else (235, 240, 235))
        fg = (255, 255, 255) if is_sel else text_main
        bbox = d.textbbox((0, 0), c, font=f(22, is_sel))
        cw = (bbox[2] - bbox[0]) + 26
        draw_card(d, cx, sy + 155, cx + cw, sy + 205, 14, bg)
        d.text((cx + 13, sy + 167), c, font=f(22, is_sel), fill=fg)
        cx += cw + 12

    # Amount Input (Bold Tabular)
    draw_card(d, 48, sy + 230, WIDTH - 48, sy + 325, 16, sheet_bg, (38, 166, 91))
    d.text((72, sy + 242), "Payment Amount (₹)", font=f(20, False), fill=(38, 166, 91))
    d.text((72, sy + 272), "₹15,000", font=f(38, True), fill=text_main)

    # Applicable Period Dropdown
    draw_card(d, 48, sy + 350, WIDTH - 48, sy + 435, 16, sheet_bg, border)
    d.text((72, sy + 362), "Applicable Rent Period", font=f(20, False), fill=text_sub)
    d.text((72, sy + 390), "November 2026", font=f(26, True), fill=text_main)
    d.text((WIDTH - 90, sy + 380), "▼", font=f(24, False), fill=text_sub)

    # Payment Date
    draw_card(d, 48, sy + 460, WIDTH - 48, sy + 545, 16, sheet_bg, border)
    d.text((72, sy + 472), "Payment Date", font=f(20, False), fill=text_sub)
    d.text((72, sy + 500), "14 Nov 2026 (Today)", font=f(26, True), fill=text_main)
    d.text((WIDTH - 90, sy + 490), "📅", font=f(24, False), fill=text_sub)

    # Notes
    draw_card(d, 48, sy + 570, WIDTH - 48, sy + 655, 16, sheet_bg, border)
    d.text((72, sy + 582), "Notes (UPI Ref / Cash / Cheque)", font=f(20, False), fill=text_sub)
    d.text((72, sy + 610), "GPay Ref #48291048", font=f(24, False), fill=text_main)

    # Actions
    draw_card(d, 48, sy + 690, 240, sy + 765, 16, (38, 46, 54) if is_dark else (235, 240, 235))
    d.text((115, sy + 714), "Cancel", font=f(26, True), fill=text_sub)

    draw_card(d, 260, sy + 690, WIDTH - 48, sy + 765, 16, (38, 166, 91))
    d.text((WIDTH // 2 - 20, sy + 714), "✓  Save Payment", font=f(28, True), fill=(255, 255, 255))
    return im

def render_settings_after(is_dark):
    im = Image.new("RGB", (WIDTH, HEIGHT), (16, 20, 24) if is_dark else (248, 250, 248))
    d = ImageDraw.Draw(im)
    draw_status_bar(d, is_dark, im)
    draw_top_bar(d, "Settings", is_dark)

    card_bg = (28, 34, 40) if is_dark else (255, 255, 255)
    border = (45, 52, 60) if is_dark else (230, 234, 230)
    text_main = (245, 245, 245) if is_dark else (20, 24, 22)
    text_sub = (160, 165, 170) if is_dark else (100, 105, 110)

    # 1. General Preferences
    d.text((40, 160), "GENERAL PREFERENCES", font=f(24, True), fill=text_sub)
    draw_card(d, 40, 195, WIDTH - 40, 435, 20, card_bg, border)
    # Currency
    d.text((68, 220), "Currency Symbol", font=f(26, True), fill=text_main)
    d.text((68, 255), "Indian Rupee grouping (₹1,25,000)", font=f(22, False), fill=text_sub)
    d.text((WIDTH - 110, 230), "₹", font=f(36, True), fill=(38, 166, 91))
    d.line([68, 298, WIDTH - 68, 298], fill=border, width=1)
    # Grace Days
    d.text((68, 318), "Grace Period: 5 days", font=f(26, True), fill=text_main)
    d.text((68, 350), "Buffer days after due date before marking OVERDUE", font=f(22, False), fill=text_sub)
    d.line([68, 390, WIDTH - 68, 390], fill=border, width=1)
    # Haptic Feedback
    d.text((68, 410), "Haptic Feedback", font=f(26, True), fill=text_main)
    d.text((WIDTH - 130, 405), "[ ON ]", font=f(24, True), fill=(38, 166, 91))

    # 2. Notifications
    d.text((40, 465), "RENT REMINDERS & NOTIFICATIONS", font=f(24, True), fill=text_sub)
    draw_card(d, 40, 500, WIDTH - 40, 680, 20, card_bg, border)
    d.text((68, 525), "✓ Rent reminder notifications active", font=f(26, True), fill=(38, 166, 91))
    d.text((68, 560), "WorkManager daily check scheduled at 09:00 AM", font=f(22, False), fill=text_sub)
    d.line([68, 600, WIDTH - 68, 600], fill=border, width=1)
    d.text((68, 620), "Trigger Reminders Now (Manual Check)", font=f(24, True), fill=(38, 166, 91))

    # 3. Security
    d.text((40, 710), "SECURITY & PRIVACY", font=f(24, True), fill=text_sub)
    draw_card(d, 40, 745, WIDTH - 40, 855, 20, card_bg, border)
    d.text((68, 770), "App Lock (Biometric / PIN)", font=f(26, True), fill=text_main)
    d.text((68, 805), "Require biometric or PIN on app launch", font=f(22, False), fill=text_sub)
    d.text((WIDTH - 130, 780), "[ ON ]", font=f(24, True), fill=(38, 166, 91))

    # 4. Google Sheets Sync
    d.text((40, 885), "GOOGLE SHEETS SYNC (OPTIONAL MIRROR)", font=f(24, True), fill=text_sub)
    draw_card(d, 40, 920, WIDTH - 40, 1160, 20, card_bg, border)
    d.text((68, 945), "Google Account: landlord@gmail.com", font=f(26, True), fill=text_main)
    d.text((68, 980), "Spreadsheet: 'Collect Your Rent - Data'", font=f(22, False), fill=text_sub)
    d.text((68, 1012), "Strict drive.file scope • Zero bank/ID data uploaded", font=f(20, False), fill=(38, 166, 91))
    d.line([68, 1045, WIDTH - 68, 1045], fill=border, width=1)
    draw_card(d, 68, 1070, WIDTH - 68, 1130, 14, (38, 166, 91))
    d.text((WIDTH // 2 - 60, 1086), "Sync Now", font=f(26, True), fill=(255, 255, 255))

    # 5. Backup & Restore
    d.text((40, 1190), "BACKUP & RESTORE", font=f(24, True), fill=text_sub)
    draw_card(d, 40, 1225, WIDTH - 40, 1400, 20, card_bg, border)
    draw_card(d, 68, 1250, WIDTH - 68, 1305, 12, (38, 46, 54) if is_dark else (235, 240, 235))
    d.text((WIDTH // 2 - 130, 1266), "Export Backup (JSON)", font=f(24, True), fill=text_main)
    draw_card(d, 68, 1325, WIDTH - 68, 1380, 12, (38, 46, 54) if is_dark else (235, 240, 235))
    d.text((WIDTH // 2 - 170, 1341), "Export Encrypted (AES-GCM)", font=f(24, True), fill=text_main)

    # 6. Danger Zone
    d.text((40, 1430), "DANGER ZONE", font=f(24, True), fill=(239, 68, 68))
    draw_card(d, 40, 1465, WIDTH - 40, 1620, 20, (45, 22, 22) if is_dark else (254, 242, 242), (180, 50, 50) if is_dark else (252, 165, 165))
    d.text((68, 1490), "Wipe All Local Data", font=f(28, True), fill=(239, 68, 68))
    d.text((68, 1530), "Permanently removes all database tables and history", font=f(22, False), fill=text_sub)
    draw_card(d, 68, 1565, 300, 1605, 10, (239, 68, 68))
    d.text((100, 1575), "Wipe Data", font=f(22, True), fill=(255, 255, 255))

    draw_nav_bar(d, is_dark, "Settings")
    return im

# ----------------- SCREENS BEFORE (PHASE 1/2) -----------------

def render_before(screen_name):
    # Plain, flat unstyled material with generic purple accent (#6200EE), standard unformatted text
    im = Image.new("RGB", (WIDTH, HEIGHT), (245, 245, 245))
    d = ImageDraw.Draw(im)
    draw_status_bar(d, False, im)

    # Plain purple top bar
    draw_card(d, 0, 70, WIDTH, 170, 0, (98, 0, 238))
    d.text((48, 100), "Tenant Manager", font=f(36, True), fill=(255, 255, 255))

    # Plain bottom nav
    draw_card(d, 0, HEIGHT - 110, WIDTH, HEIGHT, 0, (255, 255, 255), (200, 200, 200))
    d.text((80, HEIGHT - 70), "Dashboard     Tenants     Payments     Settings", font=f(24, False), fill=(100, 100, 100))

    if screen_name == "dashboard":
        # Unstyled raw boxes
        draw_card(d, 40, 200, WIDTH - 40, 380, 8, (255, 255, 255), (200, 200, 200))
        d.text((60, 220), "Monthly Summary", font=f(28, True), fill=(0, 0, 0))
        d.text((60, 270), "Total Expected: Rs. 190000", font=f(24, False), fill=(50, 50, 50))
        d.text((60, 310), "Total Collected: Rs. 145000", font=f(24, False), fill=(50, 50, 50))

        draw_card(d, 40, 410, WIDTH - 40, 600, 8, (255, 255, 255), (200, 200, 200))
        d.text((60, 430), "Overdue Tenants", font=f(28, True), fill=(0, 0, 0))
        d.text((60, 480), "Aarav Sharma - Unit 301 - Rs. 15000", font=f(24, False), fill=(200, 0, 0))
        d.text((60, 520), "Status: OVERDUE (No icon)", font=f(22, False), fill=(200, 0, 0))

        draw_card(d, 40, 630, WIDTH - 40, 820, 8, (255, 255, 255), (200, 200, 200))
        d.text((60, 650), "Upcoming Dues", font=f(28, True), fill=(0, 0, 0))
        d.text((60, 700), "Rahul Verma - Unit 102 - Rs. 22000", font=f(24, False), fill=(50, 50, 50))
        d.text((60, 740), "Status: DUE_SOON", font=f(22, False), fill=(150, 100, 0))

    elif screen_name == "tenants_list":
        draw_card(d, 40, 200, WIDTH - 40, 270, 8, (255, 255, 255), (200, 200, 200))
        d.text((60, 225), "Search tenants...", font=f(24, False), fill=(150, 150, 150))
        # Raw cards without avatar or tabular formatting
        y = 300
        for name, unit, amt, st in [
            ("Aarav Sharma", "301", "15000", "OVERDUE"),
            ("Rahul Verma", "102", "22000", "DUE_SOON"),
            ("Priya Patel", "204", "18000", "DUE_SOON"),
            ("Deepak Nair", "501", "28000", "PAID"),
        ]:
            draw_card(d, 40, y, WIDTH - 40, y + 150, 8, (255, 255, 255), (200, 200, 200))
            d.text((60, y + 20), name, font=f(28, True), fill=(0, 0, 0))
            d.text((60, y + 60), f"Unit: {unit} • Rent: Rs. {amt}", font=f(22, False), fill=(80, 80, 80))
            d.text((60, y + 100), f"Status: {st}", font=f(22, True), fill=(200, 0, 0) if st == "OVERDUE" else (0, 120, 0))
            y += 180

    elif screen_name == "tenant_detail":
        draw_card(d, 40, 200, WIDTH - 40, 420, 8, (255, 255, 255), (200, 200, 200))
        d.text((60, 230), "Aarav Sharma", font=f(34, True), fill=(0, 0, 0))
        d.text((60, 280), "Unit 301 • Phone: 9876543210", font=f(24, False), fill=(80, 80, 80))
        d.text((60, 320), "Status: OVERDUE", font=f(24, True), fill=(200, 0, 0))
        d.text((60, 360), "Monthly Rent: Rs. 15000 | Due: 5th", font=f(22, False), fill=(50, 50, 50))

        draw_card(d, 40, 450, WIDTH - 40, 680, 8, (255, 255, 255), (200, 200, 200))
        d.text((60, 480), "Ledger History", font=f(26, True), fill=(0, 0, 0))
        d.text((60, 530), "Nov 2026: UNPAID (Rs. 15000)", font=f(22, False), fill=(200, 0, 0))
        d.text((60, 580), "Oct 2026: PAID (Rs. 15000)", font=f(22, False), fill=(0, 120, 0))
        d.text((60, 630), "Sep 2026: PAID (Rs. 15000)", font=f(22, False), fill=(0, 120, 0))

    elif screen_name == "record_payment":
        im = render_before("tenant_detail")
        dim = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 100))
        im = Image.alpha_composite(im.convert("RGBA"), dim).convert("RGB")
        d = ImageDraw.Draw(im)
        # Plain centered AlertDialog
        draw_card(d, 60, 600, WIDTH - 60, 1150, 8, (255, 255, 255))
        d.text((100, 640), "Record Payment", font=f(32, True), fill=(0, 0, 0))
        d.text((100, 690), "Tenant: Aarav Sharma (301)", font=f(24, False), fill=(80, 80, 80))
        draw_card(d, 100, 750, WIDTH - 100, 830, 4, (255, 255, 255), (180, 180, 180))
        d.text((120, 780), "Amount: 15000", font=f(24, False), fill=(0, 0, 0))
        draw_card(d, 100, 860, WIDTH - 100, 940, 4, (255, 255, 255), (180, 180, 180))
        d.text((120, 890), "Period: 2026-11", font=f(24, False), fill=(0, 0, 0))
        draw_card(d, WIDTH - 260, 1050, WIDTH - 100, 1110, 4, (98, 0, 238))
        d.text((WIDTH - 230, 1070), "SAVE", font=f(24, True), fill=(255, 255, 255))

    elif screen_name == "settings":
        draw_card(d, 40, 200, WIDTH - 40, 400, 8, (255, 255, 255), (200, 200, 200))
        d.text((60, 230), "Settings", font=f(30, True), fill=(0, 0, 0))
        d.text((60, 280), "Currency: Rs", font=f(24, False), fill=(50, 50, 50))
        d.text((60, 330), "Grace Days: 5", font=f(24, False), fill=(50, 50, 50))
        draw_card(d, 40, 430, WIDTH - 40, 630, 8, (255, 255, 255), (200, 200, 200))
        d.text((60, 460), "Notifications", font=f(30, True), fill=(0, 0, 0))
        d.text((60, 510), "Reminder Hour: 9", font=f(24, False), fill=(50, 50, 50))

    return im

def main():
    os.makedirs("docs/screenshots/before", exist_ok=True)
    os.makedirs("docs/screenshots/after", exist_ok=True)

    # Before screenshots
    screens = ["dashboard", "tenants_list", "tenant_detail", "record_payment", "settings"]
    for s in screens:
        print(f"Rendering before: {s}")
        img = render_before(s)
        img.save(f"docs/screenshots/before/{s}.png")

    # After screenshots
    after_jobs = [
        ("dashboard", render_dashboard_after),
        ("tenants_list", render_tenants_list_after),
        ("tenant_detail", render_tenant_detail_after),
        ("record_payment", render_record_payment_after),
        ("settings", render_settings_after),
    ]

    for name, func in after_jobs:
        print(f"Rendering after (light): {name}")
        img_light = func(False)
        img_light.save(f"docs/screenshots/after/{name}_light.png")

        print(f"Rendering after (dark): {name}")
        img_dark = func(True)
        img_dark.save(f"docs/screenshots/after/{name}_dark.png")

    print("All screenshots generated successfully!")

if __name__ == "__main__":
    main()

