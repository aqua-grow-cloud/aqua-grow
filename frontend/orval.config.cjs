module.exports = {
    'aqua-grow': {
        output: {
            mode: 'tags-split',
            target: 'src/api/generated',
            schemas: 'src/api/generated/model',
            client: 'vue-query',
            override: {
                mutator: {
                    path: 'src/api/axios-instance.ts',
                    name: 'customInstance',
                },
            },
        },
        input: {
            target: './openapi.json',
        },
    },
};
