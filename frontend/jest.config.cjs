module.exports = {
  preset: 'jest-preset-angular',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  roots: ['<rootDir>/src'],
  testMatch: ['**/*.spec.ts'],
  collectCoverage: true,
  collectCoverageFrom: [
    'src/app/core/**/*.ts',
    'src/app/pages/**/*.ts',
    '!src/app/**/*.module.ts'
  ],
  coverageDirectory: '../couvertures/frontend',
  coverageReporters: ['text', 'lcov', 'cobertura'],
  coverageThreshold: {
    global: {
      statements: 60,
      branches: 60
    }
  },
  transform: {
    '^.+\\.(ts|mjs|js)$': [
      'jest-preset-angular',
      {
        tsconfig: '<rootDir>/tsconfig.spec.json'
      }
    ]
  },
  moduleNameMapper: {
    '\\.(html)$': '<rootDir>/src/test/mocks/htmlMock.js'
  },
  moduleFileExtensions: ['ts', 'html', 'js', 'json', 'mjs']
};
