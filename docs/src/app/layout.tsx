import type { Metadata } from 'next';
import { RootProvider } from 'fumadocs-ui/provider/next';
import { gitConfig, keywords, ogLocale, ogSiteName, orgName, siteDescription, siteUrl, tagline } from '@/lib/shared';
import './global.css';

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: {
    template: '%s | relay docs',
    default: `relay — ${tagline}`,
  },
  description: siteDescription,
  keywords,
  authors: [{ name: orgName, url: `https://github.com/${gitConfig.user}` }],
  creator: orgName,
  publisher: orgName,
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-image-preview': 'large',
    },
  },
  openGraph: {
    type: 'website',
    url: siteUrl,
    siteName: ogSiteName,
    title: `relay — ${tagline}`,
    description: siteDescription,
    locale: ogLocale,
  },
  twitter: {
    card: 'summary_large_image',
    title: `relay — ${tagline}`,
    description: siteDescription,
  },
};

const jsonLd = {
  '@context': 'https://schema.org',
  '@type': 'SoftwareApplication',
  name: 'Relay',
  applicationCategory: 'DeveloperApplication',
  operatingSystem: 'Linux, macOS, Windows (via Docker)',
  description: siteDescription,
  url: siteUrl,
  offers: {
    '@type': 'Offer',
    price: '0',
    priceCurrency: 'USD',
  },
  author: {
    '@type': 'Organization',
    name: orgName,
    url: `https://github.com/${gitConfig.user}`,
  },
};

export default function Layout({ children }: LayoutProps<'/'>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="flex flex-col min-h-screen">
        <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />
        <RootProvider>{children}</RootProvider>
      </body>
    </html>
  );
}
