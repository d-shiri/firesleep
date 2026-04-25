// TVFrame.jsx — A 1920x1080 "TV screen" with subtle bezel for Fire TV mockups.
// Scaled via CSS transform so it fits inside a DCArtboard of any size.

function TVFrame({ width = 960, height = 540, children, style = {} }) {
  // Native resolution is 1920x1080; we scale to fit the artboard width.
  const scale = width / 1920;
  return (
    <div
      style={{
        width,
        height,
        position: 'relative',
        background: '#000',
        borderRadius: 10,
        overflow: 'hidden',
        boxShadow: '0 0 0 1px rgba(255,255,255,0.06), 0 20px 60px rgba(0,0,0,0.4)',
        ...style,
      }}
    >
      <div
        style={{
          width: 1920,
          height: 1080,
          transform: `scale(${scale})`,
          transformOrigin: 'top left',
          position: 'absolute',
          top: 0,
          left: 0,
        }}
      >
        {children}
      </div>
    </div>
  );
}

window.TVFrame = TVFrame;
