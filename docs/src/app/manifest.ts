import type { MetadataRoute } from 'next';
import { tagline } from '@/lib/shared';

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: `relay — ${tagline}`,
    short_name: 'relay',
    description: 'Open-source, self-hosted webhook delivery service.',
    start_url: '/',
    display: 'standalone',
    background_color: '#0a0a0a',
    theme_color: '#0a0a0a',
    icons: [
      { src: '/icon', sizes: '32x32', type: 'image/png' },
      { src: '/apple-icon', sizes: '180x180', type: 'image/png' },
    ],
  };
}
