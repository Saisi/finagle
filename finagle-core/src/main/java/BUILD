java_library(name='java',
  provides = artifact(
    org = 'com.twitter',
    name = 'finagle-core-java',
    repo = artifactory,
  ),
  fatal_warnings=False,
  sources=rglobs('*.java'),
  dependencies=[
    '3rdparty/jvm/org/scala-lang:scala-library',
    'finagle/finagle-core/src/main/scala',
    'util/util-core/src/main/scala'
  ]
)
