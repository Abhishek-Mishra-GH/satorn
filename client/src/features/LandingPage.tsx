import { Link } from 'react-router-dom';
import type { ComponentPropsWithoutRef } from 'react';
import {
  Activity,
  ArrowRight,
  CheckCircle2,
  ChevronRight,
  Clock3,
  DatabaseZap,
  FileSearch,
  Globe2,
  Radio,
  SearchCheck,
  ShieldCheck,
  Signal,
  Sparkles,
  Zap,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/shared/store/authStore';

type MotionExtras = {
  initial?: unknown;
  animate?: unknown;
  whileInView?: unknown;
  viewport?: unknown;
  custom?: unknown;
  variants?: unknown;
};

const stripMotionProps = <T extends MotionExtras>(props: T) => {
  const { initial, animate, whileInView, viewport, custom, variants, ...rest } = props;
  void initial;
  void animate;
  void whileInView;
  void viewport;
  void custom;
  void variants;
  return rest;
};

const MotionDiv = (props: ComponentPropsWithoutRef<'div'> & MotionExtras) => <div {...stripMotionProps(props)} />;
const MotionH1 = (props: ComponentPropsWithoutRef<'h1'> & MotionExtras) => <h1 {...stripMotionProps(props)} />;
const MotionH2 = (props: ComponentPropsWithoutRef<'h2'> & MotionExtras) => <h2 {...stripMotionProps(props)} />;
const MotionP = (props: ComponentPropsWithoutRef<'p'> & MotionExtras) => <p {...stripMotionProps(props)} />;

const liveSignals = [
  { label: 'Cross-source match', value: '8 sources', tone: 'text-emerald-600' },
  { label: 'Claim confidence', value: 'Verified', tone: 'text-primary' },
  { label: 'Last checked', value: '18 sec ago', tone: 'text-cyan-700' },
];

const verificationSteps = [
  {
    icon: Radio,
    title: 'Capture the live signal',
    desc: 'SATORN continuously watches trusted news streams, RSS feeds, and emerging reports as stories break.',
  },
  {
    icon: SearchCheck,
    title: 'Triangulate every claim',
    desc: 'Facts are compared across publishers, official sources, timelines, entities, and language patterns.',
  },
  {
    icon: ShieldCheck,
    title: 'Publish with proof',
    desc: 'Readers see verification status, confidence, source trail, and why a story was cleared or challenged.',
  },
];

const trustPoints = [
  {
    icon: Globe2,
    title: 'Real-time coverage',
    desc: 'A fast news feed built for breaking updates, not a delayed study queue.',
  },
  {
    icon: FileSearch,
    title: 'Traceable evidence',
    desc: 'Each article carries source context, claim checks, and provenance markers.',
  },
  {
    icon: DatabaseZap,
    title: 'Automated verification',
    desc: 'AI checks factual consistency the moment new reports arrive.',
  },
  {
    icon: Clock3,
    title: 'Freshness signals',
    desc: 'Clear timestamps show when a story was found, checked, and refreshed.',
  },
];

export const LandingPage = () => {
  const { isAuthenticated } = useAuthStore();

  return (
    <div className="flex flex-col overflow-hidden bg-white text-foreground">
      <section className="relative min-h-[86vh] overflow-hidden border-b border-zinc-200 bg-[radial-gradient(circle_at_18%_15%,rgba(14,165,233,0.10),transparent_28%),linear-gradient(135deg,#ffffff_0%,#f8fafc_48%,#fff7ed_100%)]">
        <div className="container relative mx-auto flex min-h-[86vh] max-w-7xl flex-col justify-center px-6 py-16 lg:py-20">
          <div className="grid items-center gap-12 lg:grid-cols-[1.02fr_0.98fr]">
            <div className="max-w-3xl">
              <MotionDiv
                initial="hidden"
                animate="visible"
                custom={0}
                className="mb-7 inline-flex items-center gap-2 rounded-full border border-zinc-200 bg-white/85 px-4 py-2 text-sm font-semibold text-zinc-700 shadow-sm backdrop-blur"
              >
                <Signal className="h-4 w-4 text-emerald-600" />
                Real-time verified news platform
              </MotionDiv>

              <MotionH1
                initial="hidden"
                animate="visible"
                custom={1}
                className="max-w-4xl text-5xl font-extrabold leading-[1.02] tracking-tight text-zinc-950 sm:text-6xl lg:text-7xl"
                style={{ fontFamily: 'Outfit, sans-serif' }}
              >
                News as it breaks.
                <span className="block text-primary">Truth as it updates.</span>
              </MotionH1>

              <MotionP
                initial="hidden"
                animate="visible"
                custom={2}
                className="mt-7 max-w-2xl text-lg leading-8 text-zinc-600 sm:text-xl"
              >
                SATORN is moving to a real-time news experience where every story is checked continuously against live sources, claim evidence, and credibility signals before it reaches you.
              </MotionP>

              <MotionDiv
                initial="hidden"
                animate="visible"
                custom={3}
                className="mt-9 flex flex-col gap-3 sm:flex-row"
              >
                <Button size="lg" asChild className="h-12 rounded-full px-7 text-base shadow-lg shadow-orange-500/20">
                  <Link to={isAuthenticated ? '/feed' : '/register'}>
                    {isAuthenticated ? 'Open Live Feed' : 'Start Watching Live News'}
                    <ArrowRight className="ml-2 h-5 w-5" />
                  </Link>
                </Button>
                <Button size="lg" variant="outline" asChild className="h-12 rounded-full border-zinc-300 bg-white px-7 text-base hover:bg-zinc-50">
                  <Link to="/chat">
                    Verify a Claim
                    <ChevronRight className="ml-1 h-4 w-4" />
                  </Link>
                </Button>
              </MotionDiv>
            </div>

            <MotionDiv
              initial="hidden"
              animate="visible"
              custom={4}
              className="relative"
            >
              <div className="rounded-[2rem] border border-zinc-200 bg-white p-4 shadow-2xl shadow-zinc-300/40">
                <div className="rounded-[1.5rem] border border-zinc-100 bg-zinc-950 p-5 text-white">
                  <div className="mb-5 flex items-center justify-between border-b border-white/10 pb-4">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-cyan-300">Live verification room</p>
                      <h2 className="mt-1 text-xl font-bold">Breaking story monitor</h2>
                    </div>
                    <span className="flex items-center gap-2 rounded-full bg-emerald-400/12 px-3 py-1.5 text-xs font-semibold text-emerald-300">
                      <span className="h-2 w-2 rounded-full bg-emerald-400 shadow-[0_0_16px_rgba(52,211,153,0.9)]" />
                      Live
                    </span>
                  </div>

                  <div className="space-y-4">
                    <div className="rounded-2xl border border-white/10 bg-white/[0.06] p-4">
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <p className="text-sm font-semibold text-white">Policy update reported across national wires</p>
                          <p className="mt-2 text-sm leading-6 text-zinc-300">Checking official statement, ministry feed, wire copy, and regional coverage.</p>
                        </div>
                        <CheckCircle2 className="mt-1 h-5 w-5 flex-shrink-0 text-emerald-300" />
                      </div>
                    </div>

                    <div className="grid gap-3 sm:grid-cols-3">
                      {liveSignals.map((signal) => (
                        <div key={signal.label} className="rounded-2xl border border-white/10 bg-white/[0.04] p-3">
                          <p className="text-[11px] font-medium uppercase tracking-wide text-zinc-400">{signal.label}</p>
                          <p className={`mt-2 text-sm font-bold ${signal.tone}`}>{signal.value}</p>
                        </div>
                      ))}
                    </div>

                    <div className="space-y-3 rounded-2xl border border-white/10 bg-white/[0.04] p-4">
                      {[
                        ['Primary source detected', 'complete'],
                        ['Contradiction scan', 'complete'],
                        ['Context timeline refresh', 'running'],
                      ].map(([label, status]) => (
                        <div key={label} className="flex items-center justify-between gap-4">
                          <span className="text-sm text-zinc-300">{label}</span>
                          <span className={status === 'complete' ? 'text-emerald-300' : 'text-cyan-300'}>
                            {status === 'complete' ? <CheckCircle2 className="h-4 w-4" /> : <Activity className="h-4 w-4 animate-pulse" />}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </MotionDiv>
          </div>
        </div>
      </section>

      <section className="border-b border-zinc-200 bg-white px-6 py-10">
        <div className="container mx-auto grid max-w-6xl gap-4 sm:grid-cols-3">
          {[
            { value: '24/7', label: 'live news monitoring' },
            { value: 'Every claim', label: 'checked against source evidence' },
            { value: 'Real time', label: 'freshness and credibility signals' },
          ].map((stat, i) => (
            <MotionDiv
              key={stat.label}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true }}
              custom={i}
              className="border-l-2 border-primary/30 px-5 py-3"
            >
              <p className="text-3xl font-extrabold text-zinc-950" style={{ fontFamily: 'Outfit, sans-serif' }}>{stat.value}</p>
              <p className="mt-1 text-sm font-medium text-zinc-500">{stat.label}</p>
            </MotionDiv>
          ))}
        </div>
      </section>

      <section className="bg-zinc-50 px-6 py-24">
        <div className="container mx-auto max-w-6xl">
          <div className="mb-14 max-w-3xl">
            <MotionH2
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, margin: '-60px' }}
              custom={0}
              className="text-3xl font-extrabold tracking-tight text-zinc-950 sm:text-5xl"
              style={{ fontFamily: 'Outfit, sans-serif' }}
            >
              A verification engine built for the speed of news.
            </MotionH2>
            <MotionP
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true, margin: '-60px' }}
              custom={1}
              className="mt-5 text-lg leading-8 text-zinc-600"
            >
              SATORN does not just summarize headlines. It inspects claims, watches for contradictions, and refreshes trust signals while the story evolves.
            </MotionP>
          </div>

          <div className="grid gap-5 md:grid-cols-3">
            {verificationSteps.map((step, i) => (
              <MotionDiv
                key={step.title}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true, margin: '-40px' }}
                custom={i}
                className="rounded-2xl border border-zinc-200 bg-white p-7 shadow-sm"
              >
                <div className="mb-8 flex items-center justify-between">
                  <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
                    <step.icon className="h-6 w-6" />
                  </div>
                  <span className="text-sm font-bold text-zinc-300">0{i + 1}</span>
                </div>
                <h3 className="text-xl font-bold text-zinc-950" style={{ fontFamily: 'Outfit, sans-serif' }}>{step.title}</h3>
                <p className="mt-3 leading-7 text-zinc-600">{step.desc}</p>
              </MotionDiv>
            ))}
          </div>
        </div>
      </section>

      <section className="bg-white px-6 py-24">
        <div className="container mx-auto grid max-w-6xl gap-12 lg:grid-cols-[0.86fr_1.14fr] lg:items-center">
          <MotionDiv
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: '-60px' }}
            custom={0}
          >
            <h2 className="text-3xl font-extrabold tracking-tight text-zinc-950 sm:text-5xl" style={{ fontFamily: 'Outfit, sans-serif' }}>
              From noisy feeds to verified signals.
            </h2>
            <p className="mt-5 text-lg leading-8 text-zinc-600">
              The new SATORN experience focuses on real-time discovery, source transparency, and claim-level verification so you can move fast without trusting blind.
            </p>
            <div className="mt-8 flex flex-col gap-3 sm:flex-row">
              <Button asChild className="rounded-full px-6">
                <Link to="/feed">
                  Browse Feed <ArrowRight className="ml-2 h-4 w-4" />
                </Link>
              </Button>
              <Button variant="outline" asChild className="rounded-full border-zinc-300 px-6">
                <Link to="/chat">Open Debunk Tool</Link>
              </Button>
            </div>
          </MotionDiv>

          <div className="grid gap-4 sm:grid-cols-2">
            {trustPoints.map((point, i) => (
              <MotionDiv
                key={point.title}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true, margin: '-30px' }}
                custom={i}
                className="rounded-2xl border border-zinc-200 bg-white p-6 shadow-sm transition-shadow hover:shadow-lg hover:shadow-zinc-200/70"
              >
                <point.icon className="mb-5 h-7 w-7 text-primary" />
                <h3 className="text-lg font-bold text-zinc-950" style={{ fontFamily: 'Outfit, sans-serif' }}>{point.title}</h3>
                <p className="mt-2 text-sm leading-6 text-zinc-600">{point.desc}</p>
              </MotionDiv>
            ))}
          </div>
        </div>
      </section>

      <section className="px-6 pb-24">
        <div className="container mx-auto max-w-5xl">
          <MotionDiv
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            custom={0}
            className="relative overflow-hidden rounded-[2rem] bg-zinc-950 p-8 text-white shadow-2xl shadow-zinc-300/60 sm:p-12"
          >
            <div className="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-primary via-cyan-400 to-emerald-400" />
            <div className="grid gap-8 lg:grid-cols-[1fr_auto] lg:items-center">
              <div>
                <div className="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl bg-white/10 text-cyan-300">
                  <Zap className="h-6 w-6" />
                </div>
                <h2 className="text-3xl font-extrabold tracking-tight sm:text-5xl" style={{ fontFamily: 'Outfit, sans-serif' }}>
                  Follow the news in real time. Keep the proof in view.
                </h2>
                <p className="mt-5 max-w-2xl text-lg leading-8 text-zinc-300">
                  Start with the live feed, then use Debunk whenever a claim needs a closer look.
                </p>
              </div>
              <div className="flex flex-col gap-3 sm:flex-row lg:flex-col">
                <Button size="lg" asChild className="h-12 rounded-full bg-white px-7 text-base text-zinc-950 hover:bg-zinc-100">
                  <Link to={isAuthenticated ? '/feed' : '/register'}>
                    {isAuthenticated ? 'Open Live Feed' : 'Create Account'}
                    <ArrowRight className="ml-2 h-5 w-5" />
                  </Link>
                </Button>
                <Button size="lg" variant="outline" asChild className="h-12 rounded-full border-white/20 bg-transparent px-7 text-base text-white hover:bg-white/10 hover:text-white">
                  <Link to="/chat">
                    Verify Claim
                    <Sparkles className="ml-2 h-4 w-4" />
                  </Link>
                </Button>
              </div>
            </div>
          </MotionDiv>
        </div>
      </section>
    </div>
  );
};
