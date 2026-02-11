export const Footer = () => {
  return (
    <footer className="border-t border-warm-100 py-6 text-center text-sm text-muted-foreground bg-warm-50/30">
      <div className="container flex flex-col items-center gap-2 px-4">
        <span className="text-lg font-extrabold text-primary tracking-tight" style={{ fontFamily: 'Outfit, sans-serif' }}>
          SATORN
        </span>
        <p>&copy; {new Date().getFullYear()} SATORN — AI-Powered News Verification</p>
      </div>
    </footer>
  );
};
