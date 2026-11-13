/**
 * 店面布局/排版自动审查 (配合 ui-ux-pro-max 规范)
 * 用法: 在浏览器页面上下文执行 auditPage(), 返回该页违规列表
 * 检查项: 水平溢出 / 文字对比度 / 触控目标尺寸 / 行高 / 行长 / 标题层级 /
 *         过小字号 / 缺 alt 的图 / 文字被裁剪 / 链接与按钮的可访问名
 */
export function auditPage() {
  const violations = [];
  const push = (rule, severity, el, detail) => {
    violations.push({
      rule,
      severity,
      detail,
      tag: el ? el.tagName.toLowerCase() : null,
      cls: el ? (el.className && String(el.className).slice(0, 60)) : null,
      text: el ? (el.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 50) : null,
    });
  };

  // ---- 1. 水平溢出 ----
  const doc = document.documentElement;
  if (doc.scrollWidth > window.innerWidth + 2) {
    push('horizontal-overflow', 'high', doc, `scrollWidth ${doc.scrollWidth} > viewport ${window.innerWidth}`);
    // 找出超出右边界的元素
    for (const el of document.querySelectorAll('body *')) {
      const r = el.getBoundingClientRect();
      if (r.width > 0 && r.right > window.innerWidth + 2) {
        push('overflow-element', 'medium', el, `right=${Math.round(r.right)} > ${window.innerWidth}`);
        break;
      }
    }
  }

  // ---- 工具 ----
  const parseColor = (c) => {
    const m = c.match(/rgba?\(([^)]+)\)/);
    if (!m) return null;
    const p = m[1].split(',').map((x) => parseFloat(x));
    return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 };
  };
  const lum = ({ r, g, b }) => {
    const f = (x) => {
      x /= 255;
      return x <= 0.03928 ? x / 12.92 : Math.pow((x + 0.055) / 1.055, 2.4);
    };
    return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b);
  };
  const ratio = (c1, c2) => {
    const l1 = lum(c1), l2 = lum(c2);
    const hi = Math.max(l1, l2), lo = Math.min(l1, l2);
    return (hi + 0.05) / (lo + 0.05);
  };
  const effectiveBg = (el) => {
    let node = el;
    while (node && node !== document.documentElement) {
      const bg = parseColor(getComputedStyle(node).backgroundColor);
      if (bg && bg.a > 0.5) return bg;
      node = node.parentElement;
    }
    return { r: 255, g: 255, b: 255, a: 1 };
  };
  const isVisible = (el) => {
    const r = el.getBoundingClientRect();
    const cs = getComputedStyle(el);
    return r.width > 0 && r.height > 0 && cs.visibility !== 'hidden' && cs.display !== 'none' && cs.opacity !== '0';
  };
  const hasOwnText = (el) =>
    Array.from(el.childNodes).some((n) => n.nodeType === 3 && n.textContent.trim().length > 1);

  // ---- 2. 文字对比度 (正文 < 4.5:1, 大字 < 3:1) ----
  for (const el of document.querySelectorAll('body *')) {
    if (!hasOwnText(el) || !isVisible(el)) continue;
    const cs = getComputedStyle(el);
    const fg = parseColor(cs.color);
    if (!fg || fg.a < 0.5) continue;
    const size = parseFloat(cs.fontSize);
    const weight = parseInt(cs.fontWeight, 10) || 400;
    const large = size >= 24 || (size >= 18.66 && weight >= 700);
    const need = large ? 3 : 4.5;
    const r = ratio(fg, effectiveBg(el));
    if (r < need) {
      push('contrast', 'high', el, `${r.toFixed(2)}:1 < ${need} (${cs.color} on ${getComputedStyle(el.parentElement || el).backgroundColor}) ${Math.round(size)}px`);
    }
  }

  // ---- 3. 触控目标 (交互元素 < 24px 高/宽, WCAG 2.2 Target Size) ----
  for (const el of document.querySelectorAll('a, button, input, select, textarea, [role=button]')) {
    if (!isVisible(el)) continue;
    const r = el.getBoundingClientRect();
    if (r.width === 0 && r.height === 0) continue;
    // 内联文字链接不算目标尺寸规则
    if (el.tagName === 'A' && getComputedStyle(el).display === 'inline') continue;
    if (r.height < 24 || r.width < 24) {
      push('target-size', 'medium', el, `${Math.round(r.width)}x${Math.round(r.height)} < 24`);
    }
  }

  // ---- 4. 正文行高 (<1.5) ----
  for (const el of document.querySelectorAll('p, li, dd, .content p, .acc-body, .muted, .pdp-teaser')) {
    if (!hasOwnText(el) || !isVisible(el)) continue;
    const cs = getComputedStyle(el);
    const lh = parseFloat(cs.lineHeight);
    const fs = parseFloat(cs.fontSize);
    if (!lh || !fs) continue;
    if (lh / fs < 1.45 && fs >= 13) {
      push('line-height', 'medium', el, `line-height ${(lh / fs).toFixed(2)} < 1.5 (${Math.round(fs)}px)`);
    }
  }

  // ---- 5. 行长 (> 90 字符) ----
  for (const el of document.querySelectorAll('p, .acc-body, .content p, .pdp-teaser')) {
    if (!hasOwnText(el) || !isVisible(el)) continue;
    const r = el.getBoundingClientRect();
    const fs = parseFloat(getComputedStyle(el).fontSize);
    const chars = Math.round(r.width / (fs * 0.5));
    if (chars > 95) {
      push('line-length', 'medium', el, `~${chars} 字符/行 > 95`);
    }
  }

  // ---- 6. 标题层级 ----
  const heads = Array.from(document.querySelectorAll('h1,h2,h3,h4,h5,h6')).filter(isVisible);
  const h1s = heads.filter((h) => h.tagName === 'H1');
  if (h1s.length === 0) push('heading', 'high', document.body, '页面无 h1');
  if (h1s.length > 1) push('heading', 'medium', h1s[1], `多个 h1 (${h1s.length})`);
  let prev = 0;
  for (const h of heads) {
    const lvl = Number(h.tagName[1]);
    if (prev && lvl > prev + 1) {
      push('heading-order', 'medium', h, `h${prev} → h${lvl} 跳级`);
    }
    prev = lvl;
  }

  // ---- 7. 过小字号 (<12px 的可见文字) ----
  for (const el of document.querySelectorAll('body *')) {
    if (!hasOwnText(el) || !isVisible(el)) continue;
    const fs = parseFloat(getComputedStyle(el).fontSize);
    if (fs > 0 && fs < 12) {
      push('font-size', 'medium', el, `${Math.round(fs)}px < 12`);
    }
  }

  // ---- 8. 图片缺 alt ----
  for (const img of document.querySelectorAll('img')) {
    if (!isVisible(img)) continue;
    if (!img.hasAttribute('alt')) push('img-alt', 'medium', img, 'img 缺 alt 属性');
  }

  // ---- 9. 文字被裁剪 (非缩略图场景) ----
  for (const el of document.querySelectorAll('body *')) {
    if (!hasOwnText(el) || !isVisible(el)) continue;
    const cs = getComputedStyle(el);
    if (cs.overflow === 'hidden' && cs.textOverflow === 'ellipsis') {
      if (el.scrollWidth > el.clientWidth + 2 && el.clientWidth > 200) {
        push('clipped-text', 'medium', el, `文字被省略号裁剪 (${el.scrollWidth}>${el.clientWidth})`);
      }
    }
  }

  // ---- 10. 交互元素无可访问名 ----
  for (const el of document.querySelectorAll('a, button')) {
    if (!isVisible(el)) continue;
    const name = (el.textContent || '').trim() || el.getAttribute('aria-label') || el.getAttribute('title') || '';
    if (!name) {
      push('accessible-name', 'high', el, '交互元素无可访问名');
    }
  }

  return violations;
}
