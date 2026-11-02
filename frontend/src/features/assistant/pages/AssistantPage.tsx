import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { PageHeader } from '@/components/ui/PageHeader';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { fraudService } from '@/services/fraud/fraudService';
import {
  Bot,
  Send,
  Sparkles,
  User,
  HelpCircle,
  ShieldAlert,
  Loader2,
  AlertTriangle,
} from 'lucide-react';
import { toast } from 'sonner';

interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  text: string;
  timestamp: string;
  isAlert?: boolean;
}

export const AssistantPage: React.FC = () => {
  const { user } = useAuth();
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'assistant',
      text: `Namaste ${user?.fullName || 'Customer'}! I am your Aethel Banking AI Security Copilot powered by Google Gemini. I can assist you with understanding your transaction history, explaining held transactions, answering banking limits, and protecting you against Digital Arrest and cyber scams. How may I assist you today?`,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    },
  ]);

  const [quickPrompts, setQuickPrompts] = useState<string[]>([
    'What is a Digital Arrest scam and how do I stay safe?',
    'Why did my transfer get placed on hold?',
    'What are my daily transfer limits?',
    'Someone claiming to be CBI called me. What should I do?',
  ]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  const handleSend = async (textToSend?: string) => {
    const text = (textToSend || input).trim();
    if (!text || isLoading) return;

    const userMsg: ChatMessage = {
      id: `u-${Date.now()}`,
      sender: 'user',
      text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInput('');
    setIsLoading(true);

    try {
      const response = await fraudService.chatWithAssistant(text);

      const assistantMsg: ChatMessage = {
        id: `a-${Date.now()}`,
        sender: 'assistant',
        text: response.reply,
        timestamp: new Date(response.timestamp || Date.now()).toLocaleTimeString([], {
          hour: '2-digit',
          minute: '2-digit',
        }),
        isAlert: response.isSecurityAlert,
      };

      setMessages((prev) => [...prev, assistantMsg]);

      if (response.suggestedActions && response.suggestedActions.length > 0) {
        setQuickPrompts(response.suggestedActions);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Assistant failed to respond';
      toast.error(msg);

      const errorMsg: ChatMessage = {
        id: `err-${Date.now()}`,
        sender: 'assistant',
        text: 'I apologize, but I encountered an issue connecting to the AI Security Engine. Please verify your connection or visit the Security Center for immediate help.',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-8 max-w-4xl mx-auto">
      <PageHeader
        index="[06]"
        tag="INTELLIGENT AI AGENT"
        title="BANKING & FRAUD COPILOT"
        subtitle="AI-POWERED ASSISTANT FOR SPENDING INQUIRIES, HELD TRANSFERS & DIGITAL ARREST DEFENSE"
        statusBadge={
          <Badge variant="accent" size="sm" className="flex items-center gap-1">
            <Sparkles className="w-3 h-3" />
            <span>GEMINI AI ACTIVE</span>
          </Badge>
        }
      />

      <Card withCrosshairs className="p-0 overflow-hidden flex flex-col h-[600px]">
        {/* Chat message history */}
        <div className="flex-1 p-5 overflow-y-auto space-y-4 bg-background/50">
          {messages.map((m) => (
            <div
              key={m.id}
              className={`flex items-start gap-3 ${
                m.sender === 'user' ? 'flex-row-reverse' : ''
              }`}
            >
              <div
                className={`w-8 h-8 rounded-sm flex items-center justify-center shrink-0 text-xs font-bold ${
                  m.sender === 'user'
                    ? 'bg-accent text-background'
                    : m.isAlert
                    ? 'bg-status-danger/20 border border-status-danger/40 text-status-danger'
                    : 'bg-background-elevated border border-border text-accent'
                }`}
              >
                {m.sender === 'user' ? (
                  <User className="w-4 h-4" />
                ) : m.isAlert ? (
                  <ShieldAlert className="w-4 h-4 text-status-danger" />
                ) : (
                  <Bot className="w-4 h-4" />
                )}
              </div>

              <div
                className={`max-w-[80%] p-4 rounded-sm text-xs font-mono leading-relaxed whitespace-pre-wrap ${
                  m.sender === 'user'
                    ? 'bg-accent/15 border border-accent/30 text-foreground'
                    : m.isAlert
                    ? 'bg-status-danger/10 border border-status-danger/30 text-foreground'
                    : 'bg-background-elevated border border-border text-foreground'
                }`}
              >
                {m.isAlert && (
                  <div className="flex items-center gap-1.5 text-status-danger font-bold text-[11px] mb-2 pb-1.5 border-b border-status-danger/20">
                    <AlertTriangle className="w-3.5 h-3.5" />
                    <span>CRITICAL FRAUD / DIGITAL ARREST ADVISORY</span>
                  </div>
                )}
                <p>{m.text}</p>
                <span className="block text-[9px] text-foreground-muted text-right mt-2">
                  {m.timestamp}
                </span>
              </div>
            </div>
          ))}

          {isLoading && (
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-sm bg-background-elevated border border-border flex items-center justify-center text-accent">
                <Bot className="w-4 h-4 animate-spin" />
              </div>
              <div className="p-3 bg-background-elevated border border-border rounded-sm text-xs font-mono text-foreground-secondary flex items-center gap-2">
                <Loader2 className="w-3.5 h-3.5 animate-spin text-accent" />
                <span>Gemini is analyzing transaction context & scam heuristics...</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Quick Question Chips */}
        <div className="p-3 bg-background-secondary/60 border-t border-border flex flex-wrap gap-2">
          {quickPrompts.map((q, idx) => (
            <button
              key={idx}
              type="button"
              disabled={isLoading}
              onClick={() => handleSend(q)}
              className="px-2.5 py-1 text-[11px] font-mono rounded-sm border border-border hover:border-accent hover:text-accent transition-colors bg-background-elevated text-foreground-secondary flex items-center gap-1 disabled:opacity-50"
            >
              <HelpCircle className="w-3 h-3 text-accent" />
              <span>{q}</span>
            </button>
          ))}
        </div>

        {/* Chat input box */}
        <div className="p-4 border-t border-border bg-background flex items-center gap-2">
          <Input
            placeholder="Ask about digital arrest scams, held transfers, or spending patterns (Hindi/English)..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !isLoading) handleSend();
            }}
            disabled={isLoading}
            isMonospace
            className="flex-1"
          />
          <Button
            variant="primary"
            size="md"
            onClick={() => handleSend()}
            disabled={!input.trim() || isLoading}
            isLoading={isLoading}
            rightIcon={<Send className="w-4 h-4" />}
          >
            SEND
          </Button>
        </div>
      </Card>
    </div>
  );
};
