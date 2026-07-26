import { defineConfig } from "astro/config";
import starlight from "@astrojs/starlight";

export default defineConfig({
  base: '/docs',
  integrations: [
    starlight({
      title: "Blueprint",
      routeMiddleware: "./src/routeData.ts",
      description:
        "AI-Powered Multi-Cloud FinOps Platform — Product & System Documentation",
      logo: {
        light: "./src/assets/logo-light.svg",
        dark: "./src/assets/logo-dark.svg",
        replacesTitle: false,
      },
      social: [
        {
          icon: "github",
          label: "GitHub",
          href: "https://github.com/jawadazeem/blueprint",
        },
      ],
      customCss: ["./src/styles/custom.css"],
      head: [
        {
          tag: "script",
          attrs: { type: "module" },
          content: `
            import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
            function initMermaid() {
              const theme = document.documentElement.dataset.theme === 'dark' ? 'dark' : 'default';
              mermaid.initialize({ startOnLoad: false, theme });
              document.querySelectorAll('pre > code.language-mermaid').forEach((el) => {
                const pre = el.parentElement;
                const wrapper = document.createElement('div');
                wrapper.classList.add('mermaid');
                wrapper.textContent = el.textContent;
                pre.replaceWith(wrapper);
              });
              mermaid.run({ querySelector: '.mermaid' });
            }
            initMermaid();
            // Re-init on Starlight theme toggle (View Transitions)
            document.addEventListener('astro:after-swap', initMermaid);
          `,
        },
      ],
      sidebar: [
        { label: "Architecture", autogenerate: { directory: "architecture" } },
        { label: "Features", autogenerate: { directory: "features" } },
        { label: "API Reference", autogenerate: { directory: "api" } },
        { label: "Data Model", autogenerate: { directory: "data-model" } },
        {
          label: "Infrastructure",
          autogenerate: { directory: "infrastructure" },
        },
      ],
    }),
  ],
});
