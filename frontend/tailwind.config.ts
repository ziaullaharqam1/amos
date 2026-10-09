import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        navy: {
          950: '#06101c',
          900: '#0b1f3a',
          800: '#123056',
          700: '#1a4270',
        },
        accent: {
          400: '#2dd4bf',
          500: '#14b8a6',
          600: '#0d9488',
        },
      },
      fontFamily: {
        sans: ['Source Sans 3', 'system-ui', 'sans-serif'],
        display: ['Barlow', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
} satisfies Config
