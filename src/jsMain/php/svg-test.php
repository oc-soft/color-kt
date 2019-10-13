<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Edge svg data parsing test</title>
  <script>
    var test1Setting = <?php echo file_get_contents("svg-test-config.js"); ?>;
  </script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/gl-matrix/2.8.1/gl-matrix-min.js">
  </script>
  <script data-main="svg-test" src="https://cdnjs.cloudflare.com/ajax/libs/require.js/2.3.6/require.js">
  </script>
</head>
<body>
<ul id="test-result" sytle="list-syle-type:none;">
</ul>
<template id="test-result-item">
<li>
  <div class="test path-data"></div>
  <canvas class="test region" width="256" height="256"></canvas>
</li>
</template>
</body>
</html>
