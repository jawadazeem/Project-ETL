/*
 * (C) Copyright 2026 Jawad Azeem
 * Apache 2.0 License
 */

import { defineRouteMiddleware } from '@astrojs/starlight/route-data';

export const onRequest = defineRouteMiddleware((context) => {
    const { entry } = context.locals.starlightRoute;

    if (entry.data.hero?.actions) {
        entry.data.hero.actions = entry.data.hero.actions.map((action) => ({
            ...action,
            link: action.link.startsWith('/') && !action.link.startsWith('/docs')
                ? `/docs${action.link}`
                : action.link,
        }));
    }
});