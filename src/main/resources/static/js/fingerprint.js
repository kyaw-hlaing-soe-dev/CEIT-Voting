/**
 * Advanced Device Fingerprinting Module
 * Creates a unique device identifier that persists across:
 * - Different browsers on the same device
 * - Incognito/Private browsing mode
 * - Cookie clearing
 * - VPN usage
 *
 * Combines multiple hardware and software characteristics
 */
(function() {
  'use strict';

  /**
   * Generate canvas fingerprint - based on hardware rendering differences
   */
  const getCanvasFingerprint = () => {
    try {
      const canvas = document.createElement('canvas');
      canvas.width = 200;
      canvas.height = 50;
      const ctx = canvas.getContext('2d');

      // Draw various elements
      ctx.textBaseline = 'top';
      ctx.font = '14px Arial';
      ctx.fillStyle = '#f60';
      ctx.fillRect(125, 1, 62, 20);
      ctx.fillStyle = '#069';
      ctx.fillText('KTU Voting 🎭', 2, 15);
      ctx.fillStyle = 'rgba(102, 204, 0, 0.7)';
      ctx.fillText('Device ID', 4, 37);

      // Add gradients and arcs
      const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
      gradient.addColorStop(0, 'red');
      gradient.addColorStop(0.5, 'green');
      gradient.addColorStop(1, 'blue');
      ctx.fillStyle = gradient;
      ctx.beginPath();
      ctx.arc(50, 25, 20, 0, Math.PI * 2, true);
      ctx.fill();

      return canvas.toDataURL();
    } catch (e) {
      return 'canvas-not-supported';
    }
  };

  /**
   * Generate WebGL fingerprint - graphics card info
   */
  const getWebGLFingerprint = () => {
    try {
      const canvas = document.createElement('canvas');
      const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
      if (!gl) return 'webgl-not-supported';

      const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
      const vendor = debugInfo ? gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL) : 'unknown';
      const renderer = debugInfo ? gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) : 'unknown';

      return `${vendor}~${renderer}`;
    } catch (e) {
      return 'webgl-error';
    }
  };

  /**
   * Get audio context fingerprint
   */
  const getAudioFingerprint = () => {
    try {
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (!AudioContext) return 'audio-not-supported';

      const ctx = new AudioContext();
      const oscillator = ctx.createOscillator();
      const analyser = ctx.createAnalyser();
      const gainNode = ctx.createGain();
      const scriptProcessor = ctx.createScriptProcessor(4096, 1, 1);

      analyser.fftSize = 2048;
      oscillator.type = 'triangle';
      oscillator.frequency.setValueAtTime(10000, ctx.currentTime);

      let fingerprint = 'audio:';
      fingerprint += ctx.sampleRate + '~';
      fingerprint += ctx.destination.maxChannelCount + '~';
      fingerprint += ctx.destination.numberOfInputs + '~';
      fingerprint += ctx.destination.numberOfOutputs;

      ctx.close();
      return fingerprint;
    } catch (e) {
      return 'audio-error';
    }
  };

  /**
   * Get screen and display info
   */
  const getScreenInfo = () => {
    const screen = window.screen;
    return [
      screen.width,
      screen.height,
      screen.availWidth,
      screen.availHeight,
      screen.colorDepth,
      screen.pixelDepth,
      window.devicePixelRatio || 1
    ].join('~');
  };

  /**
   * Get hardware and platform info
   */
  const getHardwareInfo = () => {
    const nav = navigator;
    return [
      nav.hardwareConcurrency || 'unknown',
      nav.deviceMemory || 'unknown',
      nav.maxTouchPoints || 0,
      'ontouchstart' in window ? 1 : 0,
      nav.platform || 'unknown',
      nav.vendor || 'unknown'
    ].join('~');
  };

  /**
   * Get timezone and locale info
   */
  const getTimezoneInfo = () => {
    const date = new Date();
    return [
      date.getTimezoneOffset(),
      Intl.DateTimeFormat().resolvedOptions().timeZone || 'unknown',
      navigator.language || 'unknown',
      navigator.languages ? navigator.languages.join(',') : 'unknown'
    ].join('~');
  };

  /**
   * Get plugins and mime types (works better on desktop)
   */
  const getPluginsInfo = () => {
    try {
      const plugins = [];
      for (let i = 0; i < Math.min(navigator.plugins.length, 10); i++) {
        plugins.push(navigator.plugins[i].name);
      }
      return plugins.sort().join('~') || 'no-plugins';
    } catch (e) {
      return 'plugins-error';
    }
  };

  /**
   * Get font detection fingerprint
   * Detects which fonts are installed by measuring text width
   */
  const getFontFingerprint = () => {
    try {
      const testFonts = [
        'Arial', 'Verdana', 'Times New Roman', 'Georgia', 'Courier New',
        'Comic Sans MS', 'Impact', 'Lucida Console', 'Tahoma', 'Trebuchet MS',
        'Myanmar Text', 'Pyidaungsu', 'Zawgyi-One' // Myanmar fonts
      ];

      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      ctx.font = '72px monospace';
      const baseWidth = ctx.measureText('mmmmmmmmmmlli').width;

      const detected = [];
      for (const font of testFonts) {
        ctx.font = `72px "${font}", monospace`;
        if (ctx.measureText('mmmmmmmmmmlli').width !== baseWidth) {
          detected.push(font);
        }
      }

      return detected.join('~') || 'default-fonts';
    } catch (e) {
      return 'font-error';
    }
  };

  /**
   * Get storage and feature support
   */
  const getFeatureSupport = () => {
    return [
      'localStorage' in window ? 1 : 0,
      'sessionStorage' in window ? 1 : 0,
      'indexedDB' in window ? 1 : 0,
      'WebSocket' in window ? 1 : 0,
      'Worker' in window ? 1 : 0,
      document.createElement('canvas').toDataURL ? 1 : 0
    ].join('~');
  };

  /**
   * Simple hash function for fingerprint
   */
  const hashCode = (str) => {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    return Math.abs(hash).toString(36);
  };

  /**
   * SHA-256 hash for more secure fingerprint
   */
  const sha256 = async (message) => {
    const msgBuffer = new TextEncoder().encode(message);
    const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer);
    const hashArray = Array.from(new Uint8Array(hashBuffer));
    return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  };

  /**
   * Generate complete device fingerprint
   */
  const generateFingerprint = async () => {
    const components = [
      getCanvasFingerprint(),
      getWebGLFingerprint(),
      getAudioFingerprint(),
      getScreenInfo(),
      getHardwareInfo(),
      getTimezoneInfo(),
      getPluginsInfo(),
      getFontFingerprint(),
      getFeatureSupport()
    ];

    const rawFingerprint = components.join('|||');

    // Try to use SHA-256, fallback to simple hash
    try {
      const hash = await sha256(rawFingerprint);
      return 'fp-' + hash.substring(0, 32);
    } catch (e) {
      return 'fp-' + hashCode(rawFingerprint);
    }
  };

  /**
   * Get or generate device fingerprint
   * Caches in localStorage but regenerates if needed
   */
  const getDeviceFingerprint = async () => {
    const cached = localStorage.getItem('device_fingerprint');
    const cacheTime = localStorage.getItem('device_fingerprint_time');
    const now = Date.now();

    // Use cached if less than 1 hour old
    if (cached && cacheTime && (now - parseInt(cacheTime)) < 3600000) {
      return cached;
    }

    const fingerprint = await generateFingerprint();
    localStorage.setItem('device_fingerprint', fingerprint);
    localStorage.setItem('device_fingerprint_time', String(now));

    return fingerprint;
  };

  /**
   * Get individual components for debugging/verification
   */
  const getComponents = () => {
    return {
      canvas: getCanvasFingerprint().substring(0, 50) + '...',
      webgl: getWebGLFingerprint(),
      audio: getAudioFingerprint(),
      screen: getScreenInfo(),
      hardware: getHardwareInfo(),
      timezone: getTimezoneInfo(),
      plugins: getPluginsInfo(),
      fonts: getFontFingerprint(),
      features: getFeatureSupport()
    };
  };

  /**
   * Generate hardware hash - CROSS-BROWSER CONSISTENT
   * Uses only hardware characteristics that don't change between browsers:
   * - Screen resolution and color depth
   * - Hardware concurrency (CPU cores)
   * - Device memory
   * - Max touch points
   * - Platform
   * - Timezone
   * - WebGL renderer (GPU)
   */
  const generateHardwareHash = async () => {
    const screen = window.screen;
    const nav = navigator;

    // Components that are SAME across all browsers on same device
    const hardwareComponents = [
      // Screen (always same on device)
      screen.width,
      screen.height,
      screen.colorDepth,
      window.devicePixelRatio || 1,

      // Hardware (always same on device)
      nav.hardwareConcurrency || 0,
      nav.deviceMemory || 0,
      nav.maxTouchPoints || 0,
      nav.platform || 'unknown',

      // Timezone (always same on device)
      new Date().getTimezoneOffset(),
      Intl.DateTimeFormat().resolvedOptions().timeZone || 'unknown',

      // GPU (always same on device)
      getWebGLFingerprint()
    ];

    const rawHash = hardwareComponents.join('|');

    try {
      const hash = await sha256(rawHash);
      return 'hw-' + hash.substring(0, 32);
    } catch (e) {
      return 'hw-' + hashCode(rawHash);
    }
  };

  /**
   * Get screen info string for cross-browser matching
   */
  const getScreenInfoString = () => {
    const screen = window.screen;
    return `${screen.width}x${screen.height}x${screen.colorDepth}@${window.devicePixelRatio || 1}`;
  };

  /**
   * Get all device identification data for server
   */
  const getDeviceData = async () => {
    const [fingerprint, hardwareHash] = await Promise.all([
      getDeviceFingerprint(),
      generateHardwareHash()
    ]);

    return {
      fingerprint,
      hardwareHash,
      screenInfo: getScreenInfoString()
    };
  };

  // Make available globally
  window.DeviceFingerprint = {
    generate: generateFingerprint,
    get: getDeviceFingerprint,
    getComponents,
    getHardwareHash: generateHardwareHash,
    getScreenInfo: getScreenInfoString,
    getDeviceData
  };
})();

