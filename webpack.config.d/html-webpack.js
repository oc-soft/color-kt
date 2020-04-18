

/**
 * handle html webpack
 */
class HtmlWebpack {

  /**
   * constructor
   */
  constructor() {
  }


  /**
   * setup webpack configuration
   */
  setupWebpack(config) {
    this.setupHtmlPlugin(config);
    this.setupCdnPlugin(config);
  }

  /**
   * set html-webpack-plugin up
   */
  setupHtmlPlugin(config) {
    const HtmlWebpackPlugin = require('html-webpack-plugin');
    const htmlPluginConfig = {
      title: 'Mine sweeper',
      filename: 'entry.php',
      template: 'src/mswp.ejs'
    };
    const FindFile = require('find-file-up');
    htmlPluginConfig.template = FindFile.sync(htmlPluginConfig.template);

    config.plugins.push(new HtmlWebpackPlugin(htmlPluginConfig));

  }


  /**
   * set webpack-cdn-plugin up.
   */
  setupCdnPlugin(config) {
    const WebpackCdnPlugin = require('webpack-cdn-plugin');
    
    const cdnPluginConfig = {
      modules: [
        {
          name: 'webfontloader'
        },
        {
          name: 'jquery'
        },
        {
          name: 'gl-matrix'
        },
        {
          name: 'bootstrap',
          styles: [
            'css/bootstrap.min.css'
          ]
        },
        {
          name: '@popperjs/core'
        },
        {
          name: '@fortawesome/fontawesome-free',
          styles: [
            'css/fontawesome.css',
            'css/solid.css'
          ]
        },
        {
          name: '@fortawesome/fontawesome-svg-core'
        }
      ]
    };
    const FindFile = require('find-file-up');
    const path = require('path');
    let yarnLockPath = FindFile.sync('yarn.lock');
    let result = undefined;

    if (yarnLockPath) {
      const pathInfo = path.parse(yarnLockPath);
      cdnPluginConfig.pathToNodeModules = pathInfo.dir;
    }
    config.plugins.push(new WebpackCdnPlugin(cdnPluginConfig));
  }

}

(function(config) {
  const path = require('path');
  const confName = path.basename(__filename);
  if (confName == 'webpack.config.js') {
    (new HtmlWebpack()).setupWebpack(config);
  }
})(config);



// vi: se ts=2 sw=2 et: