// VariantD.jsx — "Moon phase" / divergent direction
// Philosophy: quiet, nearly-ambient. Uses a subtle warm amber accent (the only variant with color)
// and frames time as hours/minutes the TV will be "awake". A ladder layout.

function VariantD_Home({ focused = 1 }) {
  const presets = [
    { mins: 15, label: 'Quick doze', sub: 'one ad break' },
    { mins: 30, label: 'Short nap', sub: 'half an hour' },
    { mins: 45, label: 'One episode', sub: 'usual length' },
    { mins: 60, label: 'One hour', sub: 'a whole movie act' },
    { mins: 90, label: 'Long movie', sub: 'full film length' },
  ];
  // amber accent (warm) — oklch(0.78 0.08 65)
  const accent = 'oklch(0.78 0.08 65)';
  return (
    <div style={{
      width: 1920, height: 1080,
      background: '#0a0907',
      color: '#e8e0d4',
      fontFamily: 'Inter, system-ui, sans-serif',
      display: 'flex',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Very subtle amber vignette */}
      <div style={{
        position: 'absolute', inset: 0,
        background: 'radial-gradient(ellipse at 80% 10%, rgba(210,150,80,0.06), transparent 50%)',
        pointerEvents: 'none',
      }} />

      {/* Left — intro */}
      <div style={{
        width: 640,
        padding: '120px 80px 80px 120px',
        display: 'flex', flexDirection: 'column',
        borderRight: '1px solid rgba(232,224,212,0.08)',
      }}>
        <div style={{
          fontSize: 20, letterSpacing: 5, textTransform: 'uppercase',
          color: accent, fontWeight: 500, marginBottom: 80,
        }}>◐ Lullaby</div>
        <div style={{
          fontSize: 76, fontWeight: 200, letterSpacing: -2, lineHeight: 1.05,
          color: '#f2ead8',
        }}>
          Drift off.<br />
          We'll take<br />
          care of the<br />
          TV.
        </div>
        <div style={{
          fontSize: 24, color: 'rgba(232,224,212,0.55)', marginTop: 48,
          fontWeight: 300, lineHeight: 1.5, maxWidth: 400,
        }}>
          Audio and brightness fade over the final minute. No jarring shutoff.
        </div>
        <div style={{ flex: 1 }} />
        <div style={{
          fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace',
          fontSize: 20, color: 'rgba(232,224,212,0.35)',
        }}>
          ▲ ▼ Choose · ● Begin
        </div>
      </div>

      {/* Right — ladder of options */}
      <div style={{
        flex: 1,
        padding: '120px 120px 80px 80px',
        display: 'flex', flexDirection: 'column', justifyContent: 'center',
        gap: 4,
      }}>
        {presets.map((p, i) => {
          const isFocused = focused === i;
          return (
            <div key={p.mins} style={{
              display: 'grid',
              gridTemplateColumns: '180px 1fr auto',
              alignItems: 'baseline',
              gap: 40,
              padding: '24px 32px',
              borderRadius: 16,
              background: isFocused ? 'rgba(232,224,212,0.06)' : 'transparent',
              borderLeft: isFocused ? `3px solid ${accent}` : '3px solid transparent',
              transition: 'all 0.15s',
            }}>
              <div style={{
                fontSize: 88, fontWeight: 200, letterSpacing: -3, lineHeight: 1,
                color: isFocused ? '#f2ead8' : 'rgba(232,224,212,0.4)',
                fontFeatureSettings: '"tnum"',
                textAlign: 'right',
              }}>
                {p.mins}<span style={{ fontSize: 26, letterSpacing: 0, marginLeft: 6, color: 'rgba(232,224,212,0.4)' }}>m</span>
              </div>
              <div>
                <div style={{
                  fontSize: 34, fontWeight: 400,
                  color: isFocused ? '#f2ead8' : 'rgba(232,224,212,0.6)',
                }}>{p.label}</div>
                <div style={{
                  fontSize: 20, color: 'rgba(232,224,212,0.4)', marginTop: 4,
                }}>{p.sub}</div>
              </div>
              <div style={{
                fontSize: 20, color: isFocused ? accent : 'transparent',
                fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace',
                letterSpacing: 2,
              }}>
                ● BEGIN
              </div>
            </div>
          );
        })}
        <div style={{
          marginTop: 32, padding: '16px 32px',
          fontSize: 22, color: 'rgba(232,224,212,0.4)',
          fontWeight: 300,
        }}>
          + Custom length
        </div>
      </div>
    </div>
  );
}

function VariantD_Started({ minutes = 45 }) {
  const accent = 'oklch(0.78 0.08 65)';
  return (
    <div style={{
      width: 1920, height: 1080,
      background: '#0a0907',
      color: '#e8e0d4',
      fontFamily: 'Inter, system-ui, sans-serif',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      flexDirection: 'column', position: 'relative',
    }}>
      <div style={{
        position: 'absolute', inset: 0,
        background: 'radial-gradient(ellipse at 50% 50%, rgba(210,150,80,0.07), transparent 60%)',
      }} />
      {/* Moon glyph — just a crescent shape, no emoji */}
      <div style={{
        width: 120, height: 120, borderRadius: '50%',
        background: 'transparent',
        boxShadow: `inset 30px -10px 0 0 ${accent}`,
        marginBottom: 48,
        transform: 'rotate(-15deg)',
      }} />
      <div style={{
        fontSize: 22, letterSpacing: 5, textTransform: 'uppercase',
        color: accent, marginBottom: 32,
      }}>◐ Lullaby set</div>
      <div style={{
        fontSize: 260, fontWeight: 200, letterSpacing: -10, lineHeight: 1,
        color: '#f2ead8', fontFeatureSettings: '"tnum"',
      }}>{minutes}<span style={{ fontSize: 72, marginLeft: 16, color: 'rgba(232,224,212,0.5)', letterSpacing: 0 }}>min</span></div>
      <div style={{
        fontSize: 28, color: 'rgba(232,224,212,0.55)', marginTop: 40, fontWeight: 300,
      }}>
        Good night. TV off at <span style={{ color: '#f2ead8' }}>11:47 PM</span>.
      </div>
    </div>
  );
}

function VariantD_Overlay({ seconds = 45, focused = 0 }) {
  // focused: 0 = +10 min (default), 1 = Keep watching (cancel)
  const accent = 'oklch(0.78 0.08 65)';
  return (
    <div style={{
      width: 1920, height: 1080,
      background: '#000',
      position: 'relative',
      fontFamily: 'Inter, system-ui, sans-serif',
    }}>
      <div style={{
        position: 'absolute', inset: 0,
        background: 'linear-gradient(135deg, #1a1510, #0a0807 50%, #150f08)',
      }} />
      <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.55)' }} />

      {/* Top-right corner card */}
      <div style={{
        position: 'absolute',
        top: 72, right: 72,
        padding: '28px 32px',
        borderRadius: 20,
        background: 'rgba(12,10,8,0.9)',
        backdropFilter: 'blur(24px)',
        border: `1px solid ${accent}33`,
        color: '#e8e0d4',
        minWidth: 520,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 24, marginBottom: 20 }}>
          {/* Moon crescent shrinking as time passes */}
          <div style={{
            width: 72, height: 72, borderRadius: '50%',
            boxShadow: `inset ${18 + (60 - seconds) * 0.4}px -6px 0 0 ${accent}`,
            transform: 'rotate(-15deg)',
            flexShrink: 0,
          }} />
          <div style={{ flex: 1 }}>
            <div style={{
              fontSize: 16, letterSpacing: 3, textTransform: 'uppercase',
              color: accent, marginBottom: 4,
            }}>Fading to sleep</div>
            <div style={{
              fontSize: 44, fontWeight: 300, letterSpacing: -1, lineHeight: 1,
              color: '#f2ead8', fontFeatureSettings: '"tnum"',
            }}>0:{String(seconds).padStart(2, '0')}</div>
          </div>
        </div>

        {/* Two actions — clearly focusable */}
        <div style={{ display: 'flex', gap: 10 }}>
          <div style={{
            flex: 1,
            padding: '16px 18px',
            borderRadius: 12,
            background: focused === 0 ? accent : 'rgba(232,224,212,0.06)',
            color: focused === 0 ? '#0a0907' : '#e8e0d4',
            fontSize: 20, fontWeight: focused === 0 ? 600 : 400,
            textAlign: 'center',
            border: focused === 0 ? 'none' : '1px solid rgba(232,224,212,0.1)',
            boxShadow: focused === 0 ? '0 0 0 2px rgba(232,224,212,0.2)' : 'none',
          }}>
            + 10 minutes
          </div>
          <div style={{
            flex: 1,
            padding: '16px 18px',
            borderRadius: 12,
            background: focused === 1 ? accent : 'rgba(232,224,212,0.06)',
            color: focused === 1 ? '#0a0907' : '#e8e0d4',
            fontSize: 20, fontWeight: focused === 1 ? 600 : 400,
            textAlign: 'center',
            border: focused === 1 ? 'none' : '1px solid rgba(232,224,212,0.1)',
            boxShadow: focused === 1 ? '0 0 0 2px rgba(232,224,212,0.2)' : 'none',
          }}>
            Keep watching
          </div>
        </div>
        <div style={{
          fontSize: 15, color: 'rgba(232,224,212,0.4)', marginTop: 14,
          fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace',
          textAlign: 'center',
        }}>
          ◀ ▶ choose · ● confirm · ignore to sleep
        </div>
      </div>
    </div>
  );
}

// Custom length picker for Variant D
// Ladder of columns: hours + minutes, with a large preview number.
// D-pad: ◀▶ switches column, ▲▼ changes value, ● begins.
function VariantD_Custom({ hours = 1, minutes = 20, focusedCol = 1 }) {
  const accent = 'oklch(0.78 0.08 65)';
  const total = hours * 60 + minutes;

  // Helper to render a spinning reel with the current value centered,
  // prev/next values dimmed above and below.
  const Reel = ({ value, min, max, step = 1, unit, isFocused, pad = 2 }) => {
    const above = value - step >= min ? value - step : null;
    const below = value + step <= max ? value + step : null;
    return (
      <div style={{
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        padding: '20px 32px',
        borderRadius: 20,
        background: isFocused ? 'rgba(232,224,212,0.06)' : 'transparent',
        border: isFocused ? `2px solid ${accent}` : '2px solid transparent',
        minWidth: 260,
        position: 'relative',
      }}>
        {isFocused && (
          <div style={{
            position: 'absolute', top: 12, right: 16,
            fontSize: 18, color: accent, lineHeight: 1,
          }}>▲</div>
        )}
        <div style={{
          fontSize: 36, color: 'rgba(232,224,212,0.25)',
          fontFeatureSettings: '"tnum"', fontWeight: 300,
          height: 48, lineHeight: '48px',
        }}>
          {above !== null ? String(above).padStart(pad, '0') : '—'}
        </div>
        <div style={{
          fontSize: 160, fontWeight: 200, letterSpacing: -6, lineHeight: 1,
          color: '#f2ead8', fontFeatureSettings: '"tnum"',
          margin: '8px 0',
        }}>
          {String(value).padStart(pad, '0')}
        </div>
        <div style={{
          fontSize: 36, color: 'rgba(232,224,212,0.25)',
          fontFeatureSettings: '"tnum"', fontWeight: 300,
          height: 48, lineHeight: '48px',
        }}>
          {below !== null ? String(below).padStart(pad, '0') : '—'}
        </div>
        {isFocused && (
          <div style={{
            position: 'absolute', bottom: 12, right: 16,
            fontSize: 18, color: accent, lineHeight: 1,
          }}>▼</div>
        )}
        <div style={{
          fontSize: 18, letterSpacing: 4, textTransform: 'uppercase',
          color: 'rgba(232,224,212,0.45)', marginTop: 12,
        }}>{unit}</div>
      </div>
    );
  };

  // Compute the "off at" time — just a pleasant relative display
  const offAt = (() => {
    const base = new Date(2025, 0, 1, 22, 47); // 10:47 PM baseline
    base.setMinutes(base.getMinutes() + total);
    const h = base.getHours();
    const m = base.getMinutes();
    const ampm = h >= 12 ? 'PM' : 'AM';
    const hh = ((h + 11) % 12) + 1;
    return `${hh}:${String(m).padStart(2, '0')} ${ampm}`;
  })();

  return (
    <div style={{
      width: 1920, height: 1080,
      background: '#0a0907',
      color: '#e8e0d4',
      fontFamily: 'Inter, system-ui, sans-serif',
      display: 'flex', flexDirection: 'column',
      padding: '96px 120px',
      position: 'relative',
      overflow: 'hidden',
    }}>
      <div style={{
        position: 'absolute', inset: 0,
        background: 'radial-gradient(ellipse at 50% 110%, rgba(210,150,80,0.08), transparent 55%)',
        pointerEvents: 'none',
      }} />

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 20, marginBottom: 32, position: 'relative' }}>
        <div style={{
          fontSize: 20, letterSpacing: 4, textTransform: 'uppercase',
          color: 'rgba(232,224,212,0.45)',
        }}>◀ Back</div>
        <div style={{
          width: 6, height: 6, borderRadius: 999,
          background: 'rgba(232,224,212,0.3)',
        }} />
        <div style={{
          fontSize: 20, letterSpacing: 5, textTransform: 'uppercase',
          color: accent, fontWeight: 500,
        }}>Custom length</div>
      </div>

      {/* Main — reels centered */}
      <div style={{
        flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
        gap: 32, position: 'relative',
      }}>
        <Reel value={hours} min={0} max={8} unit="hours" isFocused={focusedCol === 0} pad={1} />
        <div style={{
          fontSize: 160, fontWeight: 200, letterSpacing: -4,
          color: 'rgba(232,224,212,0.3)', lineHeight: 1,
          alignSelf: 'center', marginTop: -24,
        }}>:</div>
        <Reel value={minutes} min={0} max={55} step={5} unit="minutes" isFocused={focusedCol === 1} pad={2} />

        {/* Summary on the right */}
        <div style={{
          marginLeft: 80,
          borderLeft: '1px solid rgba(232,224,212,0.1)',
          paddingLeft: 80,
          display: 'flex', flexDirection: 'column', gap: 24,
          minWidth: 360,
        }}>
          <div>
            <div style={{
              fontSize: 16, letterSpacing: 3, textTransform: 'uppercase',
              color: 'rgba(232,224,212,0.4)', marginBottom: 10,
            }}>Total</div>
            <div style={{
              fontSize: 64, fontWeight: 200, letterSpacing: -2, lineHeight: 1,
              color: '#f2ead8', fontFeatureSettings: '"tnum"',
            }}>
              {total} <span style={{ fontSize: 28, color: 'rgba(232,224,212,0.5)', letterSpacing: 0 }}>min</span>
            </div>
          </div>

          <div>
            <div style={{
              fontSize: 16, letterSpacing: 3, textTransform: 'uppercase',
              color: 'rgba(232,224,212,0.4)', marginBottom: 10,
            }}>Lights out</div>
            <div style={{
              fontSize: 40, fontWeight: 300, color: '#f2ead8', letterSpacing: -0.5,
              fontFeatureSettings: '"tnum"',
            }}>{offAt}</div>
          </div>

          <div style={{
            marginTop: 12,
            padding: '18px 28px',
            borderRadius: 12,
            background: accent,
            color: '#0a0907',
            fontSize: 24, fontWeight: 500,
            textAlign: 'center',
            letterSpacing: 0.5,
            boxShadow: '0 0 0 3px rgba(232,224,212,0.12)',
          }}>
            ● Begin
          </div>
        </div>
      </div>

      {/* Footer hints */}
      <div style={{
        display: 'flex', justifyContent: 'center', gap: 32,
        fontSize: 20, color: 'rgba(232,224,212,0.4)',
        fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace',
      }}>
        <span>◀ ▶ Switch column</span>
        <span style={{ opacity: 0.4 }}>·</span>
        <span>▲ ▼ Adjust value</span>
        <span style={{ opacity: 0.4 }}>·</span>
        <span>● Begin</span>
      </div>
    </div>
  );
}

window.VariantD_Home = VariantD_Home;
window.VariantD_Started = VariantD_Started;
window.VariantD_Overlay = VariantD_Overlay;
window.VariantD_Custom = VariantD_Custom;
