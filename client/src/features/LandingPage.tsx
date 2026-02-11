import { Link } from 'react-router-dom';
import {
  BrainCircuit, GraduationCap, TrendingUp, ArrowRight, Sparkles,
  ShieldCheck, Zap, BookOpen, BarChart3, Target, ChevronRight, Trophy
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/shared/store/authStore';
import { motion } from 'framer-motion';

const fadeUp = {
  hidden: { opacity: 0, y: 28 },
  visible: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { delay: i * 0.12, duration: 0.55, ease: 'easeOut' as const },
  }),
};

const scaleIn = {
  hidden: { opacity: 0, scale: 0.92 },
  visible: (i: number) => ({
    opacity: 1,
    scale: 1,
    transition: { delay: i * 0.1, duration: 0.5, ease: 'easeOut' as const },
  }),
};

export const LandingPage = () => {
  const { isAuthenticated } = useAuthStore();

  return (
    <div className="flex flex-col overflow-hidden">

      {/* ═══════ HERO ═══════ */}
      <section className="relative min-h-[85vh] flex items-center justify-center overflow-hidden">
        {/* Animated background elements */}
        <div className="absolute inset-0 bg-gradient-to-br from-indigo-50 via-white to-blue-50 -z-20" />
        <div className="absolute top-[-5%] right-[-5%] w-[500px] h-[500px] bg-primary/8 rounded-full blur-[100px] -z-10 animate-pulse" />
        <div className="absolute bottom-[-10%] left-[-5%] w-[600px] h-[600px] bg-blue-200/40 rounded-full blur-[120px] -z-10" />
        <div className="absolute top-[40%] left-[50%] w-[300px] h-[300px] bg-indigo-500/5 rounded-full blur-[80px] -z-10" />

        {/* Floating decorative dots */}
        <div className="absolute top-24 left-[15%] w-2 h-2 rounded-full bg-primary/30 animate-bounce" style={{ animationDelay: '0.5s' }} />
        <div className="absolute top-40 right-[20%] w-3 h-3 rounded-full bg-blue-300/40 animate-bounce" style={{ animationDelay: '1s' }} />
        <div className="absolute bottom-32 left-[25%] w-2.5 h-2.5 rounded-full bg-primary/20 animate-bounce" style={{ animationDelay: '1.5s' }} />

        <div className="flex flex-col items-center justify-center gap-8 px-4 py-20 text-center max-w-5xl mx-auto">
          <motion.div
            initial="hidden" animate="visible" custom={0} variants={fadeUp}
            className="flex items-center gap-2 rounded-full border border-blue-200 bg-white/80 backdrop-blur-sm px-5 py-2.5 text-sm font-medium text-blue-700 shadow-sm"
          >
            <Sparkles className="h-4 w-4 text-primary" />
            AI-Powered Smart Education Platform
          </motion.div>

          <motion.h1
            initial="hidden" animate="visible" custom={1} variants={fadeUp}
            className="max-w-4xl text-5xl font-extrabold tracking-tight sm:text-6xl lg:text-7xl leading-[1.1]"
            style={{ fontFamily: 'Outfit, sans-serif' }}
          >
            Ace Your Exams with
            <br />
            <span className="bg-gradient-to-r from-primary via-blue-600 to-indigo-700 bg-clip-text text-transparent">
              AI-Verified Knowledge.
            </span>
          </motion.h1>

          <motion.p
            initial="hidden" animate="visible" custom={2} variants={fadeUp}
            className="max-w-2xl text-lg sm:text-xl text-muted-foreground leading-relaxed"
          >
            SATORN transforms current affairs into a personalized learning experience. 
            Study from verified sources, take adaptive quizzes, and track your mastery to crack competitive exams.
          </motion.p>

          <motion.div
            initial="hidden" animate="visible" custom={3} variants={fadeUp}
            className="flex flex-col sm:flex-row gap-4 mt-4"
          >
            {isAuthenticated ? (
              <Button size="lg" asChild className="rounded-full px-10 py-6 text-base shadow-xl shadow-primary/25 hover:shadow-primary/40 transition-shadow">
                <Link to="/learning">
                  Go to Dashboard <ArrowRight className="ml-2 h-5 w-5" />
                </Link>
              </Button>
            ) : (
              <>
                <Button size="lg" asChild className="rounded-full px-10 py-6 text-base shadow-xl shadow-primary/25 hover:shadow-primary/40 transition-shadow">
                  <Link to="/register">
                    Start Learning — Free <ArrowRight className="ml-2 h-5 w-5" />
                  </Link>
                </Button>
                <Button size="lg" variant="outline" asChild className="rounded-full px-10 py-6 text-base border-blue-200 hover:bg-blue-50">
                  <Link to="/learning/feed">
                    Explore Content <ChevronRight className="ml-1 h-4 w-4" />
                  </Link>
                </Button>
              </>
            )}
          </motion.div>

          <motion.p
            initial="hidden" animate="visible" custom={4} variants={fadeUp}
            className="text-xs text-muted-foreground mt-2"
          >
            Ideal for UPSC, SSC, and Banking Aspirants • Trusted Content
          </motion.p>
        </div>
      </section>

      {/* ═══════ SOCIAL PROOF STATS ═══════ */}
      <section className="relative -mt-8 z-10 px-4">
        <motion.div
          initial="hidden" whileInView="visible" viewport={{ once: true }}
          className="container mx-auto max-w-4xl"
        >
          <motion.div
            custom={0} variants={scaleIn}
            className="bg-white rounded-3xl shadow-xl shadow-blue-200/30 border border-blue-100 p-8 sm:p-10"
          >
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-6 sm:gap-8">
              {[
                { value: '10k+', label: 'Questions Attempted', icon: BrainCircuit },
                { value: '100+', label: 'Verified Topics', icon: ShieldCheck },
                { value: '24/7', label: 'AI Tutor Support', icon: GraduationCap },
              ].map((stat, i) => (
                <motion.div key={stat.label} custom={i + 1} variants={fadeUp} className="text-center">
                  <stat.icon className="h-6 w-6 text-primary mx-auto mb-2" />
                  <p className="text-3xl sm:text-4xl font-extrabold text-foreground" style={{ fontFamily: 'Outfit, sans-serif' }}>
                    {stat.value}
                  </p>
                  <p className="text-xs sm:text-sm text-muted-foreground mt-1">{stat.label}</p>
                </motion.div>
              ))}
            </div>
          </motion.div>
        </motion.div>
      </section>

      {/* ═══════ HOW IT WORKS ═══════ */}
      <section className="px-4 py-24">
        <div className="container mx-auto max-w-6xl">
          <motion.div
            initial="hidden" whileInView="visible" viewport={{ once: true, margin: "-60px" }}
            className="text-center mb-16"
          >
            <motion.span custom={0} variants={fadeUp} className="inline-flex items-center rounded-full bg-primary/10 px-4 py-1.5 text-xs font-semibold text-primary mb-4">
              Your Path to Success
            </motion.span>
            <motion.h2
              custom={1} variants={fadeUp}
              className="text-3xl sm:text-4xl font-bold tracking-tight mb-4"
              style={{ fontFamily: 'Outfit, sans-serif' }}
            >
              Master Current Affairs in 3 Steps
            </motion.h2>
            <motion.p custom={2} variants={fadeUp} className="text-muted-foreground max-w-xl mx-auto text-lg">
              We streamline your preparation by focusing on what's important and verified.
            </motion.p>
          </motion.div>

          <div className="grid gap-6 sm:gap-8 md:grid-cols-3">
            {[
              {
                step: '01',
                icon: BookOpen,
                title: 'Learn from Verified Sources',
                desc: 'Read curated articles that are fact-checked by AI. No more misinformation or biased narratives cluttering your study material.',
                gradient: 'from-blue-500 to-indigo-500',
              },
              {
                step: '02',
                icon: Target,
                title: 'Practice Adaptively',
                desc: 'Take AI-generated quizzes that adapt to your level. We identify your weak areas and generate questions to help you improve.',
                gradient: 'from-indigo-500 to-purple-500',
              },
              {
                step: '03',
                icon: BarChart3,
                title: 'Track & Master',
                desc: 'Visualize your progress with our Skills Matrix. See your mastery grow topic by topic and stay exam-ready.',
                gradient: 'from-purple-500 to-pink-500',
              },
            ].map((item, i) => (
              <motion.div
                key={item.step}
                initial="hidden" whileInView="visible"
                viewport={{ once: true, margin: "-40px" }}
                custom={i}
                variants={fadeUp}
                className="group relative bg-white rounded-3xl p-8 border border-blue-100 shadow-sm hover:shadow-xl hover:border-blue-200 transition-all duration-500"
              >
                <div className="flex items-center gap-3 mb-6">
                  <div className={`flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br ${item.gradient} shadow-lg`}>
                    <item.icon className="h-6 w-6 text-white" />
                  </div>
                  <span className="text-5xl font-extrabold text-blue-100 group-hover:text-blue-200 transition-colors" style={{ fontFamily: 'Outfit, sans-serif' }}>
                    {item.step}
                  </span>
                </div>
                <h3 className="text-xl font-bold mb-3" style={{ fontFamily: 'Outfit, sans-serif' }}>
                  {item.title}
                </h3>
                <p className="text-muted-foreground leading-relaxed">
                  {item.desc}
                </p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══════ FEATURES BENTO GRID ═══════ */}
      <section className="bg-gradient-to-b from-blue-50/80 to-white px-4 py-24 border-t border-blue-100">
        <div className="container mx-auto max-w-6xl">
          <motion.div
            initial="hidden" whileInView="visible" viewport={{ once: true, margin: "-60px" }}
            className="text-center mb-16"
          >
            <motion.span custom={0} variants={fadeUp} className="inline-flex items-center rounded-full bg-primary/10 px-4 py-1.5 text-xs font-semibold text-primary mb-4">
              Features
            </motion.span>
            <motion.h2
              custom={1} variants={fadeUp}
              className="text-3xl sm:text-4xl font-bold tracking-tight mb-4"
              style={{ fontFamily: 'Outfit, sans-serif' }}
            >
              Everything you need to stay ahead
            </motion.h2>
            <motion.p custom={2} variants={fadeUp} className="text-muted-foreground max-w-xl mx-auto text-lg">
              From personalized feeds to an AI Tutor — SATORN has you covered.
            </motion.p>
          </motion.div>

          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {[
              {
                icon: GraduationCap,
                title: 'Personalized Profile',
                desc: 'Set your exam track, target dates, and difficulty levels. SATORN adapts the content to match your goals.',
                color: 'bg-indigo-50 text-indigo-600',
              },
              {
                icon: BrainCircuit,
                title: 'AI Tutor',
                desc: 'Stuck on a concept? Chat with our AI Tutor to get instant explanations and examples related to your syllabus.',
                color: 'bg-purple-50 text-purple-600',
              },
              {
                icon: Zap,
                title: 'Knowledge Feed',
                desc: 'A smart feed of articles prioritized by your weak areas. Stop scrolling aimlessly and start learning.',
                color: 'bg-yellow-50 text-yellow-600',
              },
              {
                icon: Trophy,
                title: 'Adaptive Quizzes',
                desc: 'Quizzes that get harder as you get better. Challenge yourself with fresh questions generated from the latest news.',
                color: 'bg-orange-50 text-orange-600',
              },
              {
                icon: TrendingUp,
                title: 'Skill Tracking',
                desc: 'Visual analytics to show your strong and weak zones. Focus your efforts where they matter most.',
                color: 'bg-emerald-50 text-emerald-600',
              },
              {
                icon: ShieldCheck,
                title: 'Verified Content',
                desc: 'Every piece of information is cross-referenced for accuracy. Build your knowledge on a foundation of truth.',
                color: 'bg-blue-50 text-blue-600',
              },
            ].map((feature, i) => (
              <motion.div
                key={feature.title}
                initial="hidden" whileInView="visible"
                viewport={{ once: true, margin: "-30px" }}
                custom={i}
                variants={fadeUp}
                className="bg-white rounded-2xl p-7 border border-blue-100 hover:shadow-lg hover:border-blue-200 transition-all duration-300 group"
              >
                <div className={`inline-flex h-12 w-12 items-center justify-center rounded-xl ${feature.color} mb-5 group-hover:scale-110 transition-transform duration-300`}>
                  <feature.icon className="h-6 w-6" />
                </div>
                <h3 className="text-lg font-bold mb-2" style={{ fontFamily: 'Outfit, sans-serif' }}>
                  {feature.title}
                </h3>
                <p className="text-sm text-muted-foreground leading-relaxed">
                  {feature.desc}
                </p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══════ FINAL CTA ═══════ */}
      <section className="px-4 py-24">
        <div className="container mx-auto max-w-4xl">
          <motion.div
            initial="hidden" whileInView="visible" viewport={{ once: true }}
            custom={0} variants={scaleIn}
            className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-primary via-blue-600 to-indigo-700 p-12 sm:p-16 text-center text-white shadow-2xl shadow-primary/30"
          >
            <div className="absolute top-0 right-0 w-64 h-64 bg-white/5 rounded-full -translate-y-32 translate-x-32 blur-2xl" />
            <div className="absolute bottom-0 left-0 w-48 h-48 bg-white/5 rounded-full translate-y-24 -translate-x-24 blur-2xl" />

            <div className="relative z-10">
              <motion.h2
                custom={1} variants={fadeUp}
                className="text-3xl sm:text-4xl lg:text-5xl font-extrabold mb-6 leading-tight"
                style={{ fontFamily: 'Outfit, sans-serif' }}
              >
                Ready to crack your exam?
              </motion.h2>
              <motion.p
                custom={2} variants={fadeUp}
                className="text-lg text-white/80 max-w-xl mx-auto mb-10"
              >
                Join thousands of aspirants who are learning smarter with SATORN.
              </motion.p>
              <motion.div custom={3} variants={fadeUp} className="flex flex-col sm:flex-row gap-4 justify-center">
                {isAuthenticated ? (
                  <Button size="lg" asChild className="rounded-full px-10 py-6 text-base bg-white text-primary hover:bg-white/90 shadow-xl">
                    <Link to="/learning">
                      Go to Dashboard <ArrowRight className="ml-2 h-5 w-5" />
                    </Link>
                  </Button>
                ) : (
                  <>
                    <Button size="lg" asChild className="rounded-full px-10 py-6 text-base bg-white text-primary hover:bg-white/90 shadow-xl">
                      <Link to="/register">
                        Create Free Account <ArrowRight className="ml-2 h-5 w-5" />
                      </Link>
                    </Button>
                    <Button size="lg" variant="outline" asChild className="rounded-full px-10 py-6 text-base border-white/30 text-white hover:text-primary bg-primary">
                      <Link to="/learning/feed">Explore Feed</Link>
                    </Button>
                  </>
                )}
              </motion.div>
            </div>
          </motion.div>
        </div>
      </section>
    </div>
  );
};
